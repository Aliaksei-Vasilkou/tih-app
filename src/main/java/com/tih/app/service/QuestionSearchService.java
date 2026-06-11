package com.tih.app.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import com.tih.app.dto.PageResponse;
import com.tih.app.dto.QuestionDto;
import com.tih.app.model.QuestionDocument;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;

/**
 * Elasticsearch-backed search service.
 * <p>
 * Scoring layers — applied as bool/should, highest boost wins but all matching layers accumulate:
 * <p>
 * Layer | Query type                        | Field(s)                | Boost    | Purpose
 * ------+-----------------------------------+-------------------------+----------+--------------------------------
 * 1     | match_phrase                      | questionText            |  ×12     | Exact phrase in question (highest — title is primary signal)
 * 2     | match AND (all words required)    | questionText            |  ×10     | Every query word in question
 * 3     | match_phrase                      | answerContent           |  ×8      | Exact phrase in answer (secondary)
 * 4     | match OR  (≥50 % words required)  | questionText            |  ×6      | Partial title match + BM25 frequency
 * 5     | match AND (all words required)    | answerContent           |  ×5      | Every query word in answer
 * 6     | match OR  (≥50 % words required)  | answerContent           |  ×3      | BM25 term-frequency for answer
 * 7     | multi_match most_fields           | questionText + answer   |  ×2      | Terms spread across both fields
 * 8     | multi_match fuzzy AUTO            | *.fuzzy                 |  ×2/½    | Typo tolerance
 * 9     | multi_match ngram                 | *.ngram                 |  ×½/⅕    | Internal substring hits
 * 10    | multi_match most_fields synonym   | *.synonym               |  ×10/×4  | Domain synonym expansion (raised from ×1.5/×0.75)
 * 11    | multi_match best_fields edge      | *.edge                  |  ×⅓/⅛    | Prefix / autocomplete hits
 * <p>
 * Key design decisions:
 * - Layer 1 gives the highest boost to exact phrase matches in the question title, ranking
 * documents whose title directly addresses the query above all other signals.
 * - Layers 1+2 combined mean a title-match always outscores a purely answer-based match.
 * - Layers 5+6 use operator:OR with minimumShouldMatch="50%" so each additional
 * matching word raises the BM25 score, while documents with fewer than half the
 * words present don't surface at all through these layers.  BM25 naturally encodes
 * term-frequency, satisfying the "frequency in answer" ranking requirement.
 * - Layer 10 uses search-time synonym expansion via the english_synonym analyzer so that
 * queries like "gc" also surface documents containing "garbage collection", without
 * requiring a re-index when the synonym list changes.  Boost raised to ×10/×4 so
 * synonym-matched title cards rank on par with direct title matches.
 * - Layer 11 indexes edge n-grams (prefix tokens) for prefix / autocomplete matching.
 * - Filters (languageId, categoryId) are zero-score; they narrow results without
 * affecting ranking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionSearchService {

    //ID of the "General" language — always included alongside any selected language filter.
    private static final long GENERAL_LANGUAGE_ID = 1L;
    private static final String QUESTION_TEXT = "questionText";
    private static final String ANSWER_CONTENT = "answerContent";

    private final ElasticsearchOperations elasticsearchOperations;

    public PageResponse<QuestionDto> search(String queryText, Long languageId, Long categoryId, Pageable pageable) {
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(buildQuery(queryText, languageId, categoryId))
                .withPageable(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()))
                .withSort(Sort.by(Sort.Order.desc("_score")))
                .withTrackScores(true)
                .build();

        SearchHits<QuestionDocument> hits = elasticsearchOperations.search(nativeQuery, QuestionDocument.class);
        log.debug("ES search for '{}': {} total hits", queryText, hits.getTotalHits());

        List<QuestionDto> dtos = hits.getSearchHits().stream()
                .map(hit -> toDto(hit.getContent(), hit.getScore()))
                .toList();

        long totalHits = hits.getTotalHits();
        int pageSize = pageable.getPageSize();
        int pageNum = pageable.getPageNumber();
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalHits / pageSize) : 0;

        return PageResponse.<QuestionDto>builder()
                .content(dtos)
                .page(pageNum)
                .size(pageSize)
                .totalElements(totalHits)
                .totalPages(totalPages)
                .last(pageNum >= totalPages - 1)
                .build();
    }

    private Query buildQuery(String text, Long languageId, Long categoryId) {
        return Query.of(q -> q
                .bool(b -> {

                    // 1. Exact phrase in question — highest boost (title is the primary relevance signal)
                    b.should(s -> s.matchPhrase(mp -> mp
                            .field(QUESTION_TEXT).query(text).boost(12.0f)));

                    // 2. All words present in question (AND) — full coverage bonus
                    b.should(s -> s.match(m -> m
                            .field(QUESTION_TEXT).query(text)
                            .operator(Operator.And).boost(10.0f)));

                    // 3. Exact phrase in answer — secondary signal
                    b.should(s -> s.matchPhrase(mp -> mp
                            .field(ANSWER_CONTENT).query(text).boost(8.0f)));

                    // 4. Most words in question (OR, ≥50%) — more words = higher BM25 score
                    b.should(s -> s.match(m -> m
                            .field(QUESTION_TEXT).query(text)
                            .minimumShouldMatch("50%").boost(6.0f)));

                    // 5. All words present in answer (AND)
                    b.should(s -> s.match(m -> m
                            .field(ANSWER_CONTENT).query(text)
                            .operator(Operator.And).boost(5.0f)));

                    // 6. Most words in answer (OR, ≥50%) — BM25 term-frequency encodes answer frequency
                    b.should(s -> s.match(m -> m
                            .field(ANSWER_CONTENT).query(text)
                            .minimumShouldMatch("50%").boost(3.0f)));

                    // 7. Terms spread across question + answer — rewards broad topic coverage
                    b.should(s -> s.multiMatch(mm -> mm
                            .fields(List.of("questionText^2", "answerContent^1"))
                            .query(text)
                            .type(TextQueryType.MostFields)));

                    // 7b. Tag synonym match — explicit subject tag is a strong relevance signal.
                    //     Boost ×8 sits between exact title phrase (×12) and answer phrase (×8),
                    //     so a tagged card outranks an answer-only match but defers to a direct title hit.
                    b.should(s -> s.multiMatch(mm -> mm
                            .fields(List.of("tags.synonym^8"))
                            .query(text)
                            .type(TextQueryType.MostFields)));

                    // 8. Fuzzy — typo tolerance on stemmer-free field (fuzziness=AUTO: 0 edits ≤2 chars,
                    //    1 edit 3-5 chars, 2 edits ≥6 chars)
                    b.should(s -> s.multiMatch(mm -> mm
                            .fields(List.of("questionText.fuzzy^2", "answerContent.fuzzy^0.5"))
                            .query(text)
                            .fuzziness("AUTO")
                            .prefixLength(1)
                            .maxExpansions(50)));

                    // 9. N-gram — partial / internal substring word hits
                    b.should(s -> s.multiMatch(mm -> mm
                            .fields(List.of("questionText.ngram^0.5", "answerContent.ngram^0.2"))
                            .query(text)
                            .type(TextQueryType.BestFields)));

                    // 10. Synonym expansion — search-time synonym_graph analyzer maps "gc" → "garbage
                    //     collection", "concurrency" → "multithreading", etc. without requiring reindex.
                    //     Boost raised to ×10/×4 so a synonym-matched title card scores on par with a
                    //     direct title match (previously ×1.5/×0.75 caused synonym matches to rank below
                    //     partial answer-body hits).
                    b.should(s -> s.multiMatch(mm -> mm
                            .fields(List.of("questionText.synonym^10", "answerContent.synonym^4"))
                            .query(text)
                            .type(TextQueryType.MostFields)));

                    // 11. Edge n-gram — prefix / autocomplete hits (e.g. "garb" matches "garbage")
                    b.should(s -> s.multiMatch(mm -> mm
                            .fields(List.of("questionText.edge^0.3", "answerContent.edge^0.15"))
                            .query(text)
                            .type(TextQueryType.BestFields)));

                    // At least one should clause must match
                    b.minimumShouldMatch("1");

                    // Zero-score filters
                    if (languageId != null) {
                        List<FieldValue> languageValues = languageId == GENERAL_LANGUAGE_ID
                                ? List.of(FieldValue.of(languageId))
                                : List.of(FieldValue.of(languageId), FieldValue.of(GENERAL_LANGUAGE_ID));
                        b.filter(f -> f.terms(t -> t
                                .field("languageId")
                                .terms(tv -> tv.value(languageValues))));
                    }
                    if (categoryId != null) {
                        b.filter(f -> f.term(t -> t
                                .field("categoryId").value(FieldValue.of(categoryId))));
                    }

                    return b;
                })
        );
    }

    private QuestionDto toDto(QuestionDocument doc, float score) {
        return QuestionDto.builder()
                .id(Long.parseLong(doc.getId()))
                .questionText(doc.getQuestionText())
                .answerContent(doc.getAnswerContent())
                .languageId(doc.getLanguageId())
                .languageName(doc.getLanguageName())
                .languageCode(doc.getLanguageCode())
                .categoryId(doc.getCategoryId())
                .categoryName(doc.getCategoryName())
                .relevanceScore(score)
                .build();
    }
}
