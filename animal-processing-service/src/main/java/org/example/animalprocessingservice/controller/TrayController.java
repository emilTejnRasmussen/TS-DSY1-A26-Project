package org.example.animalprocessingservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.animalprocessingservice.dto.CreateTrayRequest;
import org.example.animalprocessingservice.dto.TrayResponse;
import org.example.animalprocessingservice.entity.PartType;
import org.example.animalprocessingservice.entity.TrayStatus;
import org.example.animalprocessingservice.service.TrayService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trays")
@RequiredArgsConstructor
public class TrayController
{
    private final TrayService trayService;

    @PostMapping
    public ResponseEntity<TrayResponse> registerTray(@Valid @RequestBody CreateTrayRequest request)
    {
        TrayResponse response = trayService.createTray(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{trayId}")
    public ResponseEntity<TrayResponse> getTray(@PathVariable Long trayId)
    {
        TrayResponse response = trayService.getTray(trayId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<TrayResponse>> getTrays(
            @RequestParam(required = false) PartType type,
            @RequestParam(required = false) TrayStatus status,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    )
    {
        Page<TrayResponse> response = trayService.getTrays(type, status, pageable);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{trayId}/parts/{partId}")
    public ResponseEntity<TrayResponse> addPart(@PathVariable Long trayId,
                                                @PathVariable Long partId)
    {
        TrayResponse response = trayService.addPart(trayId, partId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{trayId}/parts/{partId}")
    public ResponseEntity<TrayResponse> removePart(@PathVariable Long trayId,
                                                   @PathVariable Long partId)
    {
        TrayResponse response = trayService.removePart(trayId, partId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{trayId}/seal")
    public ResponseEntity<TrayResponse> sealTray(@PathVariable Long trayId)
    {
        TrayResponse response = trayService.sealTray(trayId);

        return ResponseEntity.ok(response);
    }

}
