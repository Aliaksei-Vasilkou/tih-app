package com.tih.app.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.tih.app.dto.PageResponse;
import com.tih.app.dto.QuestionDto;
import com.tih.app.model.QuestionDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Elasticsearch-backed search service.
 *
 * Scoring layers — applied as bool/should, highest boost wins but all matching layers accumulate:
 *
 *   Layer | Query type                        | Field(s)              | Boost | Purpose
 *   ------+-----------------------------------+-----------------------+-------+----------------------------
 *     1   | match_phrase                      | questionText          |  ×10  | Exact phrase in question
 *     2   | match_phrase                      | answerContent         |  ×4   | Exact phrase in answer
 *     3   | match AND (all words required)    | questionText          |  ×6   | Every query word in question
 *     4   | match AND (all words required)    | answerContent         |  ×2   | Every query word in answer
 *     5   | match OR  (≥50 % words required)  | questionText          |  ×4   | More words → higher BM25 score
 *     6   | match OR  (≥50 % words required)  | answerContent         |  ×1.5 | Same, weighted lower
 *     7   | multi_match most_fields           | questionText + answer |  ×2   | Terms spread across both fields
 *     8   | multi_match fuzzy AUTO            | *.fuzzy               |  ×2/½ | Typo tolerance
 *     9   | multi_match ngram                 | *.ngram               |  ×½/⅕ | Partial / substring hits
 *
 * Key design decisions:
 *   - Layers 3+4 give a strong bonus when ALL query words are present.
 *   - Layers 5+6 use operator:OR with minimumShouldMatch="50%" so each additional
 *     matching word raises the BM25 score, while documents with fewer than half the
 *     words present don't surface at all through these layers.
 *   - BM25 (term frequency + inverse document frequency) is Elasticsearch's default scorer
 *     and naturally rewards higher term frequency in both layers 5 and 6.
 *   - Filters (languageId, categoryId) are zero-score; they narrow results without
 *     affecting ranking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionSearchService {

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
        int pageSize   = pageable.getPageSize();
        int pageNum    = pageable.getPageNumber();
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

    // ------------------------------------------------------------------ query

    private Query buildQuery(String text, Long languageId, Long categoryId) {
        return Query.of(q -> q
                .bool(b -> {

                    // 1. Exact phrase in question — strongest signal
                    b.should(s -> s.matchPhrase(mp -> mp
                            .field("questionText").query(text).boost(10.0f)));

                    // 2. Exact phrase in answer
                    b.should(s -> s.matchPhrase(mp -> mp
                            .field("answerContent").query(text).boost(4.0f)));

                    // 3. All words present in question (AND) — full coverage bonus
                    b.should(s -> s.match(m -> m
                            .field("questionText").query(text)
                            .operator(Operator.And).boost(6.0f)));

                    // 4. All words present in answer (AND)
                    b.should(s -> s.match(m -> m
                            .field("answerContent").query(text)
                            .operator(Operator.And).boost(2.0f)));

                    // 5. Most words in question (OR, ≥50%) — more words = higher BM25 score
                    b.should(s -> s.match(m -> m
                            .field("questionText").query(text)
                            .minimumShouldMatch("50%").boost(4.0f)));

                    // 6. Most words in answer (OR, ≥50%)
                    b.should(s -> s.match(m -> m
                            .field("answerContent").query(text)
                            .minimumShouldMatch("50%").boost(1.5f)));

                    // 7. Terms spread across question + answer — rewards broad topic coverage
                    b.should(s -> s.multiMatch(mm -> mm
                            .fields(List.of("questionText^2", "answerContent^1"))
                            .query(text)
                            .type(TextQueryType.MostFields)));

                    // 8. Fuzzy — typo tolerance on stemmer-free field
                    b.should(s -> s.multiMatch(mm -> mm
                            .fields(List.of("questionText.fuzzy^2", "answerContent.fuzzy^0.5"))
                            .query(text)
                            .fuzziness("AUTO")
                            .prefixLength(1)
                            .maxExpansions(50)));

                    // 9. N-gram — partial / substring word hits
                    b.should(s -> s.multiMatch(mm -> mm
                            .fields(List.of("questionText.ngram^0.5", "answerContent.ngram^0.2"))
                            .query(text)
                            .type(TextQueryType.BestFields)));

                    // At least one should clause must match
                    b.minimumShouldMatch("1");

                    // Zero-score filters
                    if (languageId != null) {
                        b.filter(f -> f.term(t -> t
                                .field("languageId").value(FieldValue.of(languageId))));
                    }
                    if (categoryId != null) {
                        b.filter(f -> f.term(t -> t
                                .field("categoryId").value(FieldValue.of(categoryId))));
                    }

                    return b;
                })
        );
    }

    // ------------------------------------------------------------------ mapping

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
