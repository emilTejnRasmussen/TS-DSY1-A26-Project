package org.example.animalregistrationservice.enitity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.UUID;

@Setter
@Getter
@Entity
@NoArgsConstructor
public class Animal
{
    @Id
    @Column(nullable = false, updatable = false)
    private String registrationNumber;

    @Column(nullable = false)
    private LocalDate arrivalDate;

    @Column(nullable = false)
    private BigDecimal weight;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm origin;

    public void generateRegistrationNumber(){
        if (registrationNumber != null) return;

        registrationNumber = "DK-%d-%s".formatted(Year.now().getValue(), UUID.randomUUID());
    }
}
