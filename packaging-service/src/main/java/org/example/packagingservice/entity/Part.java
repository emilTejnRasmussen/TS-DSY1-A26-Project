package org.example.packagingservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@Entity
public class Part
{
    @Id
    private Long id;

    @Column(nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal weight;

    @Column(nullable = false, updatable = false)
    private String animalRegistrationNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tray_id", nullable = false, updatable = false)
    private Tray tray;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
}
