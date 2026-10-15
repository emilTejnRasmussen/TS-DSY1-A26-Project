package org.example.animalregistrationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.animalregistrationservice.dto.farm.CreateFarmDto;
import org.example.animalregistrationservice.dto.farm.FarmDto;
import org.example.animalregistrationservice.service.FarmService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/farms")
@RequiredArgsConstructor
public class FarmController
{

    private final FarmService farmService;

    @PostMapping
    public ResponseEntity<FarmDto> registerFarm(
            @Valid @RequestBody CreateFarmDto request)
    {

        FarmDto farm = farmService.createFarm(request);

        log.info(
                "Registered farm: id={}, name={}",
                farm.id(),
                farm.name()
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(farm.id())
                .toUri();

        return ResponseEntity.created(location).body(farm);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FarmDto> getFarm(
            @PathVariable Long id)
    {

        return ResponseEntity.ok(farmService.getFarm(id));
    }

    @GetMapping
    public ResponseEntity<Page<FarmDto>> getFarms(
            @RequestParam(required = false) String nameContains,
            @PageableDefault(size = 10, sort = "name") Pageable pageable)
    {

        return ResponseEntity.ok(farmService.getFarms(nameContains, pageable));
    }
}