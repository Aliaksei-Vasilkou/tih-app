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
import java.util.ArrayList;
import java.util.List;

/**
 * Elasticsearch document for full-text search over questions.
 * <p>
 * Field strategy — questionText / answerContent each carry four sub-fields:
 *   .main    — english_standard analyzer  (stem + stopwords)  primary relevance scoring
 *   .ngram   — ngram_analyzer             (2-10 char n-grams)  internal substring matching
 *   .fuzzy   — english_lowercase          (no stemming)        fuzzy / typo-tolerance at query time
 *   .synonym — english_standard (index) / english_synonym (search)  synonym expansion at search time
 *   .edge    — edge_ngram_analyzer (index) / english_lowercase (search)  prefix / autocomplete matching
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

    // Main analyzed field + sub-fields for partial, fuzzy, synonym and prefix matching
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
                    ),
                    @InnerField(
                            suffix = "synonym",
                            type = FieldType.Text,
                            analyzer = "english_standard",
                            searchAnalyzer = "english_synonym"
                    ),
                    @InnerField(
                            suffix = "edge",
                            type = FieldType.Text,
                            analyzer = "edge_ngram_analyzer",
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
                    ),
                    @InnerField(
                            suffix = "synonym",
                            type = FieldType.Text,
                            analyzer = "english_standard",
                            searchAnalyzer = "english_synonym"
                    ),
                    @InnerField(
                            suffix = "edge",
                            type = FieldType.Text,
                            analyzer = "edge_ngram_analyzer",
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

    @Field(type = FieldType.Keyword)
    @Builder.Default
    private List<String> tags = new ArrayList<>();
}
