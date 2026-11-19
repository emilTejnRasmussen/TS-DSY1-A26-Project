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
public class Tray
{
    @Id
    @Column(name = "tray_id")
    private Long id;

    @OneToMany
    @JoinTable(name = "tray_part",
            joinColumns = @JoinColumn(name = "tray_id"),
            inverseJoinColumns = @JoinColumn(name = "part_id", unique = true))
    private Set<Part> parts = new HashSet<>();
}
