package com.company.app.ordermanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Provides auditing capabilities for entities.
 * <p>
 * The {@code Auditable} class is a base class designed to automatically track audit information
 * such as the creation timestamp and the last modification timestamp for entities in the system.
 * Classes extending {@code Auditable} will inherit these properties, which are managed
 * automatically by Spring Data JPA.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SuperBuilder
public abstract class Auditable {
    @CreatedDate
    @Column(name = "created_at", columnDefinition = "TIMESTAMP")
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP")
    private Instant updatedAt;
}
