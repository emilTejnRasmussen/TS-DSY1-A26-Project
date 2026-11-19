package org.example.traceabilityservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Part
{
    @Id
    @Column(name = "part_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "animal_registration_number", nullable = false, updatable = false)
    private Animal animal;

    @Enumerated(EnumType.STRING)
    private PartType type;

    @Column(precision = 10, scale = 2)
    private BigDecimal weight;
}
