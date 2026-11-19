package org.example.animalprocessingservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.animalprocessingservice.dto.PartDto;
import org.example.animalprocessingservice.dto.RegisterPartDto;
import org.example.animalprocessingservice.entity.PartType;
import org.example.animalprocessingservice.service.PartService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/parts")
@RequiredArgsConstructor
public class PartController
{
    private final PartService partService;

    @PostMapping
    public ResponseEntity<PartDto> registerPart(@Valid @RequestBody RegisterPartDto request)
    {
        PartDto response = partService.registerPart(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{partId}")
    public ResponseEntity<PartDto> getPart(@PathVariable Long partId)
    {
        PartDto response = partService.getPart(partId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<PartDto>> getParts(
            @RequestParam(required = false) String animalRegistrationNumber,
            @RequestParam(required = false) PartType partType,
            @PageableDefault(
                    size = 20,
                    sort = "registeredAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    )
    {
        Page<PartDto> response = partService.getParts(
                animalRegistrationNumber,
                partType,
                pageable
        );

        return ResponseEntity.ok(response);
    }
}
