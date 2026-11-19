package org.example.packagingservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@Entity
public class Tray
{
    @Id
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private PartType type;

    @Column(nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal totalWeight;

    @Column(nullable = false, updatable = false)
    private Instant sealedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @OneToMany(mappedBy = "tray", cascade = CascadeType.PERSIST)
    @OrderBy("id ASC")
    private List<Part> parts = new ArrayList<>();

    public BigDecimal getAvailableWeight()
    {
        return parts.stream()
                .filter(part -> part.getProduct() == null)
                .map(Part::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
