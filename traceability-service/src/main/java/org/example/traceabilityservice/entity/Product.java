package org.example.traceabilityservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Product
{
    @Id
    @Column(name = "product_id")
    private Long id;

    @OneToMany
    @JoinTable(name = "product_part",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "part_id", unique = true))
    private Set<Part> parts = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "product_tray",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "tray_id"))
    private Set<Tray> trays = new HashSet<>();
}
