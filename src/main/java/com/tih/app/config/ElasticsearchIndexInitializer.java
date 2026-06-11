package com.tih.app.config;

import com.tih.app.model.Question;
import com.tih.app.model.QuestionDocument;
import com.tih.app.repository.QuestionRepository;
import com.tih.app.service.QuestionIndexService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Recreates the Elasticsearch index on every startup to guarantee that
 * the custom settings (analyzers, ngram filter) and field mappings from
 * {@link QuestionDocument} are always applied correctly.
 * PostgreSQL is the source of truth, so all documents are re-indexed afterwards.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchIndexInitializer implements ApplicationRunner {

    private final QuestionRepository questionRepository;
    private final QuestionIndexService questionIndexService;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void run(ApplicationArguments args) {
        IndexOperations indexOps = elasticsearchOperations.indexOps(QuestionDocument.class);

        if (indexOps.exists()) {
            indexOps.delete();
            log.info("Dropped existing questions index");
        }

        // Creates the index applying @Setting (custom analyzers) and @Field mappings
        indexOps.createWithMapping();
        log.info("Created questions index with custom analyzers and field mappings");

        List<Question> questions = questionRepository.findAllForExport(null, null);
        if (questions.isEmpty()) {
            log.info("No questions to index");
            return;
        }
        questionIndexService.reindexAll(questions);
    }
}
