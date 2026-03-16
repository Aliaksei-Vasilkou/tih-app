package com.tih.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;

/**
 * Elasticsearch document for full-text search over questions.
 * <p>
 * Field strategy:
 *   - questionText / answerContent each have two sub-fields:
 *       .main  — english_standard analyzer  (stemming + stopwords, used for scored matching)
 *       .ngram — ngram_analyzer            (3-4 char ngrams, used for partial / prefix matching)
 *   - Fuzzy matching is applied at query time (no extra field needed).
 * </p>
 */
@Document(indexName = "questions")
@Setting(settingPath = "elasticsearch/settings.json")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String externalId;

    // Main analyzed field + ngram sub-field for partial matching
    @MultiField(
            mainField = @Field(
                    type = FieldType.Text,
                    analyzer = "english_standard",
                    searchAnalyzer = "english_standard"
            ),
            otherFields = {
                    @InnerField(
                            suffix = "ngram",
                            type = FieldType.Text,
                            analyzer = "ngram_analyzer",
                            searchAnalyzer = "english_lowercase"
                    ),
                    @InnerField(
                            suffix = "fuzzy",
                            type = FieldType.Text,
                            analyzer = "english_lowercase",
                            searchAnalyzer = "english_lowercase"
                    )
            }
    )
    private String questionText;

    @MultiField(
            mainField = @Field(
                    type = FieldType.Text,
                    analyzer = "english_standard",
                    searchAnalyzer = "english_standard"
            ),
            otherFields = {
                    @InnerField(
                            suffix = "ngram",
                            type = FieldType.Text,
                            analyzer = "ngram_analyzer",
                            searchAnalyzer = "english_lowercase"
                    ),
                    @InnerField(
                            suffix = "fuzzy",
                            type = FieldType.Text,
                            analyzer = "english_lowercase",
                            searchAnalyzer = "english_lowercase"
                    )
            }
    )
    private String answerContent;

    @Field(type = FieldType.Long)
    private Long languageId;

    @Field(type = FieldType.Keyword)
    private String languageCode;

    @Field(type = FieldType.Keyword)
    private String languageName;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Keyword)
    private String categoryName;
}
