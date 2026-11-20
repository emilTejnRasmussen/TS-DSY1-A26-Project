package org.example.packagingservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.packagingservice.dto.TrayResponse;
import org.example.packagingservice.entity.PartType;
import org.example.packagingservice.service.TrayService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trays")
@RequiredArgsConstructor
public class TrayController
{
    private final TrayService trayService;

    @GetMapping("/{trayId}")
    public ResponseEntity<TrayResponse> getTray(@PathVariable Long trayId)
    {
        return ResponseEntity.ok(trayService.getTray(trayId));
    }

    @GetMapping
    public ResponseEntity<Page<TrayResponse>> getTrays(
            @RequestParam(required = false) PartType type,
            @RequestParam(defaultValue = "true") boolean availableOnly,
            @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC)
            Pageable pageable)
    {
        return ResponseEntity.ok(trayService.getTrays(type, availableOnly, pageable));
    }
}
