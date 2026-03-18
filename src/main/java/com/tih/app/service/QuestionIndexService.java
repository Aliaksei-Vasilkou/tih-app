package com.tih.app.service;

import com.tih.app.model.Question;
import com.tih.app.model.QuestionDocument;
import com.tih.app.model.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Manages syncing Question entities from PostgreSQL into the Elasticsearch index.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionIndexService {

    private final ElasticsearchOperations elasticsearchOperations;

    public void index(Question question) {
        elasticsearchOperations.save(toDocument(question));
        log.debug("Indexed question id={}", question.getId());
    }

    public void delete(Long questionId) {
        elasticsearchOperations.delete(String.valueOf(questionId), QuestionDocument.class);
        log.debug("Removed question id={} from index", questionId);
    }

    /** Bulk upsert — used on startup to sync any questions already in PostgreSQL. */
    public void reindexAll(List<Question> questions) {
        List<QuestionDocument> docs = questions.stream().map(this::toDocument).toList();
        elasticsearchOperations.save(docs);
        log.info("Reindexed {} questions in Elasticsearch", docs.size());
    }

    private QuestionDocument toDocument(Question q) {
        List<String> tagNames = (q.getTags() != null)
                ? q.getTags().stream().map(Tag::getName).sorted().toList()
                : Collections.emptyList();
        return QuestionDocument.builder()
                .id(String.valueOf(q.getId()))
                .externalId(q.getExternalId() != null ? q.getExternalId().toString() : null)
                .questionText(q.getQuestionText())
                .answerContent(q.getAnswerContent())
                .languageId(q.getLanguage() != null ? q.getLanguage().getId() : null)
                .languageCode(q.getLanguage() != null ? q.getLanguage().getCode() : null)
                .languageName(q.getLanguage() != null ? q.getLanguage().getName() : null)
                .categoryId(q.getCategory() != null ? q.getCategory().getId() : null)
                .categoryName(q.getCategory() != null ? q.getCategory().getName() : null)
                .tags(tagNames)
                .build();
    }
}
