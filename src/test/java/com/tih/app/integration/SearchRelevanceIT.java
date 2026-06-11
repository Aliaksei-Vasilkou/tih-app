package com.tih.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.tih.app.dto.PageResponse;
import com.tih.app.dto.QuestionDto;
import com.tih.app.model.QuestionDocument;
import com.tih.app.service.QuestionSearchService;

/**
 * Integration tests verifying Elasticsearch search relevance ranking for TIH-053.
 * <p>
 * Each test scenario covers one acceptance criterion:
 * - T004/US1 : gc → GC-titled card ranks first  (synonym expansion + title boost)
 * - T006/US2 : title-match card outscores answer-only card
 * - T008/US3 : vthread / postgres → synonym expansion returns relevant cards
 * - T011/US4 : tagged card outranks untagged answer-mention card
 * <p>
 * Uses real Elasticsearch and PostgreSQL containers. The index is torn down and
 * recreated before each test to guarantee a clean, predictable document set.
 */
@SpringBootTest
@Testcontainers
class SearchRelevanceIT {

    private static final String ES_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:8.15.0";

    @Container
    @ServiceConnection
    static ElasticsearchContainer elasticsearch =
            new ElasticsearchContainer(ES_IMAGE)
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("discovery.type", "single-node");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private ElasticsearchOperations esOps;

    @Autowired
    private QuestionSearchService searchService;

    @BeforeEach
    void setUp() {
        IndexOperations indexOps = esOps.indexOps(QuestionDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.createWithMapping();
    }

    @AfterEach
    void tearDown() {
        esOps.indexOps(QuestionDocument.class).delete();
    }

    /**
     * T004 (ES path): A query of "gc" must return the card whose title contains
     * "Garbage Collection" as the first result, ahead of cards that only mention
     * garbage collection in the answer body.
     * Validates: synonym expansion (gc → garbage collection) + title phrase boost ×12.
     */
    @Test
    void gc_query_titleCard_ranksFirst() {
        // given
        QuestionDocument titleCard = doc("1",
                "What is Garbage Collection in the JVM?",
                "Garbage collection is the process of automatically freeing memory.",
                List.of("Garbage Collection", "JVM"));

        QuestionDocument answerOnlyCard = doc("2",
                "How does the JVM manage memory?",
                "The JVM uses garbage collection to reclaim unused heap objects. " +
                        "The GC algorithm runs in the background.",
                List.of("JVM"));

        index(titleCard, answerOnlyCard);

        // when
        PageResponse<QuestionDto> results =
                searchService.search("gc", null, null, PageRequest.of(0, 10));

        // then
        assertThat(results.getContent()).isNotEmpty();
        assertThat(results.getContent().getFirst().getQuestionText())
                .containsIgnoringCase("garbage collection");
    }

    /**
     * T006 (US2): A card with the query phrase in its question title must score
     * strictly higher than a card where the phrase appears only in the answer body.
     * Validates: title phrase boost (×12) > answer phrase boost (×8).
     */
    @Test
    void titleCard_scoresHigherThan_answerOnlyCard() {
        // given
        QuestionDocument titleCard = doc("10",
                "What is garbage collection?",
                "Garbage collection frees unused heap memory automatically.",
                List.of());

        QuestionDocument answerOnlyCard = doc("11",
                "Describe JVM internals",
                "The JVM manages heap memory through garbage collection. " +
                        "Garbage collection runs periodically to reclaim objects.",
                List.of());

        index(titleCard, answerOnlyCard);

        // when
        PageResponse<QuestionDto> results =
                searchService.search("garbage collection", null, null, PageRequest.of(0, 10));

        // then
        assertThat(results.getContent()).hasSizeGreaterThanOrEqualTo(2);
        List<QuestionDto> content = results.getContent();

        int titleIdx = indexOf(content, "10");
        int answerOnlyIdx = indexOf(content, "11");

        assertThat(titleIdx)
                .as("title-match card (id=10) should appear before answer-only card (id=11)")
                .isLessThan(answerOnlyIdx);

        float titleScore = content.get(titleIdx).getRelevanceScore();
        float answerScore = content.get(answerOnlyIdx).getRelevanceScore();
        assertThat(titleScore)
                .as("title-match score (%s) should be > answer-only score (%s)", titleScore, answerScore)
                .isGreaterThan(answerScore);
    }

    /**
     * T008a (US3): "vthread" must surface cards that contain "virtual thread" or
     * "project loom" — synonym group defined in elasticsearch/settings.json.
     * Validates: synonym expansion for the vthread/virtual-threads synonym group.
     */
    @Test
    void vthread_query_returnsVirtualThreadsCard() {
        // given
        QuestionDocument virtualThreadsCard = doc("20",
                "What are Virtual Threads in Java?",
                "Virtual threads, introduced via Project Loom, are lightweight threads " +
                        "managed by the JVM. They use structured concurrency.",
                List.of("Virtual Threads", "Project Loom"));

        QuestionDocument unrelatedCard = doc("21",
                "What is method overriding?",
                "Method overriding allows a subclass to redefine a method.",
                List.of());

        index(virtualThreadsCard, unrelatedCard);

        // when
        PageResponse<QuestionDto> results =
                searchService.search("vthread", null, null, PageRequest.of(0, 10));

        // then
        assertThat(results.getContent())
                .as("vthread query should return at least one result")
                .isNotEmpty();
        assertThat(results.getContent())
                .extracting(QuestionDto::getId)
                .as("virtual threads card (id=20) must appear in vthread results")
                .contains(20L);
    }

    /**
     * T008b (US3): "postgres" must surface cards that contain "PostgreSQL" —
     * synonym group defined in elasticsearch/settings.json.
     * Validates: synonym expansion for the postgresql/postgres/pg synonym group.
     */
    @Test
    void postgres_query_returnsPostgreSQLCard() {
        // given
        QuestionDocument postgresCard = doc("30",
                "What are the key features of PostgreSQL?",
                "PostgreSQL (also known as Postgres) is an advanced open-source relational database. " +
                        "It supports MVCC, JSONB, and full-text search using tsvector.",
                List.of("PostgreSQL", "Database"));

        QuestionDocument unrelatedCard = doc("31",
                "What is a REST API?",
                "REST is an architectural style for designing networked applications.",
                List.of());

        index(postgresCard, unrelatedCard);

        // when
        PageResponse<QuestionDto> results =
                searchService.search("postgres", null, null, PageRequest.of(0, 10));

        // then
        assertThat(results.getContent())
                .as("postgres query should return at least one result")
                .isNotEmpty();
        assertThat(results.getContent())
                .extracting(QuestionDto::getId)
                .as("PostgreSQL card (id=30) must appear in postgres results")
                .contains(30L);
    }

    /**
     * T011 (US4): A card explicitly tagged "Garbage Collection" must rank above
     * a card that only mentions garbage collection in its answer body, when querying "gc".
     * Validates: tags.synonym boost ×8 elevates tagged cards above untagged answer matches.
     */
    @Test
    void taggedCard_ranksAbove_untaggedAnswerMention() {
        // given
        // Card A: tagged "Garbage Collection" but the title and answer contain NO GC keywords.
        // Only tags.synonym fires for this card (boost ×8, short field → high BM25 norm).
        QuestionDocument taggedCard = doc("40",
                "How does the JVM manage heap memory?",
                "The JVM allocates objects on the heap and tracks their reachability automatically.",
                List.of("Garbage Collection", "JVM"));

        // Card B: no tag, answer has ONE "garbage collection" mention (no literal "GC" / "G1GC"
        // tokens that would fire direct match layers ×8/×5/×3 and swamp the synonym score).
        // Only answerContent.synonym fires (boost ×4) — always less than tags.synonym (×8).
        QuestionDocument untaggedCard = doc("41",
                "What happens to unused objects in the JVM?",
                "Unused objects are reclaimed through garbage collection. " +
                        "This process frees heap memory so new allocations can succeed.",
                List.of());

        // Third document keeps IDF non-trivial; 2-doc corpus gives IDF ≈ 0.18
        // which makes scores fragile. With 3 docs (df=2) IDF ≈ 0.47.
        QuestionDocument unrelatedCard = doc("42",
                "What are SOLID principles in object-oriented design?",
                "SOLID is an acronym for five design principles that help produce " +
                        "maintainable and flexible software.",
                List.of("OOP", "Design Patterns"));

        index(taggedCard, untaggedCard, unrelatedCard);

        // when
        PageResponse<QuestionDto> results =
                searchService.search("gc", null, null, PageRequest.of(0, 10));

        // then
        assertThat(results.getContent())
                .as("gc query should return both cards")
                .hasSizeGreaterThanOrEqualTo(2);

        int taggedIdx = indexOf(results.getContent(), "40");
        int untaggedIdx = indexOf(results.getContent(), "41");

        assertThat(taggedIdx)
                .as("tagged card (id=40) should appear before untagged answer-mention card (id=41)")
                .isLessThan(untaggedIdx);
    }

    private void index(QuestionDocument... docs) {
        esOps.save(List.of(docs));
        esOps.indexOps(QuestionDocument.class).refresh();
    }

    private QuestionDocument doc(String id, String question, String answer, List<String> tags) {
        return QuestionDocument.builder()
                .id(id)
                .questionText(question)
                .answerContent(answer)
                .tags(tags)
                .build();
    }

    private int indexOf(List<QuestionDto> content, String id) {
        long longId = Long.parseLong(id);
        for (int i = 0; i < content.size(); i++) {
            if (longId == content.get(i).getId()) {
                return i;
            }
        }
        throw new AssertionError("Document id=" + id + " not found in search results: " + content);
    }
}
