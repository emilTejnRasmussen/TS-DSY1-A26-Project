package org.example.traceabilityservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Animal
{
    @Id
    @Column(name = "registration_number", length = 100)
    private String registrationNumber;
}
