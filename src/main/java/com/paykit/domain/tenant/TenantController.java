package com.paykit.domain.tenant;

import com.paykit.domain.tenant.dto.CreateTenantRequest;
import com.paykit.domain.tenant.dto.TenantResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<TenantResponse> create(@Valid @RequestBody CreateTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.create(request));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<TenantResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(tenantService.getBySlug(slug));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<TenantResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.getById(id));
    }
}
