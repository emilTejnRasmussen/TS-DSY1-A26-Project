package org.example.packagingservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Entity
public class Product
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "product")
    @OrderBy("id ASC")
    private List<Part> parts = new ArrayList<>();

    public BigDecimal getTotalWeight()
    {
        return parts.stream()
                .map(Part::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
