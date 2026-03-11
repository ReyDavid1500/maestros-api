package com.maestros.controller;

import com.maestros.dto.request.CreateMaestroProfileRequest;
import com.maestros.dto.request.UpdateMaestroProfileRequest;
import com.maestros.dto.request.UpdateServicesRequest;
import com.maestros.dto.response.ApiResponse;
import com.maestros.dto.response.MaestroListItemResponse;
import com.maestros.dto.response.MaestroProfileResponse;
import com.maestros.model.MaestroSearchFilters;
import com.maestros.model.postgres.User;
import com.maestros.security.AuthHelper;
import com.maestros.service.MaestroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/maestros")
@RequiredArgsConstructor
public class MaestroController {

    private final MaestroService maestroService;
    private final AuthHelper authHelper;

    // -------------------------------------------------------------------------
    // Public endpoints
    // -------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<ApiResponse<Page<MaestroListItemResponse>>> listMaestros(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        size = Math.min(size, 50);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "averageRating", "totalJobs"));

        MaestroSearchFilters filters = new MaestroSearchFilters();
        if (categoryId != null) {
            filters.setCategoryId(UUID.fromString(categoryId));
        }
        filters.setCity(city);
        filters.setMinRating(minRating);
        filters.setMaxPriceClp(maxPrice);

        Page<MaestroListItemResponse> result = maestroService.listMaestros(filters, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<MaestroListItemResponse>>> searchMaestros(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if ((q == null || q.isBlank()) && category == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Debes proporcionar al menos un término de búsqueda"));
        }

        size = Math.min(size, 50);
        Pageable pageable = PageRequest.of(page, size);

        Page<MaestroListItemResponse> result = maestroService.searchMaestros(q, category, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaestroProfileResponse>> getMaestroDetail(@PathVariable String id) {
        UUID maestroId = UUID.fromString(id);
        MaestroProfileResponse response = maestroService.getMaestroDetail(maestroId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // -------------------------------------------------------------------------
    // Protected endpoints — MAESTRO only
    // -------------------------------------------------------------------------

    @PostMapping("/me/profile")
    @PreAuthorize("hasRole('MAESTRO')")
    public ResponseEntity<ApiResponse<MaestroProfileResponse>> createMyProfile(
            @Valid @RequestBody CreateMaestroProfileRequest request) {
        User current = authHelper.getCurrentUser();
        MaestroProfileResponse response = maestroService.createMyProfile(current.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Perfil creado exitosamente"));
    }

    @PutMapping("/me/profile")
    @PreAuthorize("hasRole('MAESTRO')")
    public ResponseEntity<ApiResponse<MaestroProfileResponse>> updateMyProfile(
            @Valid @RequestBody UpdateMaestroProfileRequest request) {
        User current = authHelper.getCurrentUser();
        MaestroProfileResponse response = maestroService.updateMyProfile(current.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Perfil actualizado"));
    }

    @PutMapping("/me/services")
    @PreAuthorize("hasRole('MAESTRO')")
    public ResponseEntity<ApiResponse<MaestroProfileResponse>> updateMyServices(
            @Valid @RequestBody UpdateServicesRequest request) {
        User current = authHelper.getCurrentUser();
        MaestroProfileResponse response = maestroService.updateMyServices(current.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Servicios actualizados"));
    }
}
