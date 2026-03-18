package com.tih.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tags",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "language_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;
}
