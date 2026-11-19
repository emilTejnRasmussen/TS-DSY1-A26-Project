package org.example.animalprocessingservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Entity
public class Tray
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal maxWeight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrayStatus status = TrayStatus.OPEN;

    @OneToMany(mappedBy = "tray")
    private List<Part> parts = new ArrayList<>();

    public BigDecimal getCurrentWeight()
    {
        return parts.stream()
                .map(Part::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
