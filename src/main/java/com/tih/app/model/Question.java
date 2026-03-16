package com.tih.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.util.UUID;

@Entity
@Table(name = "questions",
       indexes = {
           @Index(name = "idx_questions_language_id", columnList = "language_id"),
           @Index(name = "idx_questions_category_id", columnList = "category_id"),
           @Index(name = "idx_questions_active", columnList = "active"),
           @Index(name = "idx_questions_external_id", columnList = "external_id")
       })
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true, updatable = false, nullable = false)
    private UUID externalId;

    @PrePersist
    private void ensureExternalId() {
        if (externalId == null) {
            externalId = UUID.randomUUID();
        }
    }

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "answer_content", columnDefinition = "TEXT")
    private String answerContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Full-text search vector maintained by PostgreSQL trigger.
     * Not managed by JPA - read-only mapping.
     */
    @NotAudited
    @Column(name = "search_vector", columnDefinition = "tsvector", insertable = false, updatable = false)
    private String searchVector;
}
