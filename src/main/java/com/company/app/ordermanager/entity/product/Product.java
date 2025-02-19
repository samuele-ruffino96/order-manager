package com.company.app.ordermanager.entity.product;

import com.company.app.ordermanager.entity.common.Auditable;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a product available in the system.
 * <p>
 * The {@code Product} class encapsulates details about a product, including its unique identifier,
 * name, description, price, stock level, and version for optimistic locking.
 * It extends the {@link Auditable} class to provide audit details such as creation and modification timestamps.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id", "name", "description"}, callSuper = false)
@ToString
@SuperBuilder
@Entity
@Table(name = "products")
public class Product extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Version
    private long version;

    @Column(name = "stock_level", nullable = false)
    private int stockLevel;
}
