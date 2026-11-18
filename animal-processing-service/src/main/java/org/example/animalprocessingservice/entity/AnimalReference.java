
package org.example.animalprocessingservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@Entity
public class AnimalReference
{
    @Id
    @Column(nullable = false, updatable = false)
    private String registrationNumber;
}
