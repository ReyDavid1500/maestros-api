package com.maestros.controller;

import com.maestros.dto.request.CreateServiceRequestRequest;
import com.maestros.dto.response.ApiResponse;
import com.maestros.dto.response.ServiceRequestResponse;
import com.maestros.model.enums.UserRole;
import com.maestros.model.postgres.User;
import com.maestros.security.AuthHelper;
import com.maestros.service.ServiceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-requests")
@RequiredArgsConstructor
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;
    private final AuthHelper authHelper;

    @PostMapping
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> createServiceRequest(
            @Valid @RequestBody CreateServiceRequestRequest request) {
        User current = authHelper.getCurrentUser();
        ServiceRequestResponse response = serviceRequestService.createServiceRequest(current.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Solicitud creada exitosamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ServiceRequestResponse>>> getMyServiceRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User current = authHelper.getCurrentUser();
        UserRole role = current.getRole();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<ServiceRequestResponse> result = serviceRequestService.getMyServiceRequests(
                current.getId(), role, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> getServiceRequestDetail(
            @PathVariable String id) {
        User current = authHelper.getCurrentUser();
        ServiceRequestResponse response = serviceRequestService.getServiceRequestDetail(
                UUID.fromString(id), current.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}/accept")
    @PreAuthorize("hasRole('MAESTRO')")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> acceptRequest(@PathVariable String id) {
        User current = authHelper.getCurrentUser();
        ServiceRequestResponse response = serviceRequestService.acceptRequest(
                UUID.fromString(id), current.getId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Solicitud aceptada"));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('MAESTRO')")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> rejectRequest(@PathVariable String id) {
        User current = authHelper.getCurrentUser();
        ServiceRequestResponse response = serviceRequestService.rejectRequest(
                UUID.fromString(id), current.getId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Solicitud rechazada"));
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasRole('MAESTRO')")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> startWork(@PathVariable String id) {
        User current = authHelper.getCurrentUser();
        ServiceRequestResponse response = serviceRequestService.startWork(
                UUID.fromString(id), current.getId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Trabajo iniciado"));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('MAESTRO')")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> completeWork(@PathVariable String id) {
        User current = authHelper.getCurrentUser();
        ServiceRequestResponse response = serviceRequestService.completeWork(
                UUID.fromString(id), current.getId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Trabajo completado"));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> cancelRequest(@PathVariable String id) {
        User current = authHelper.getCurrentUser();
        ServiceRequestResponse response = serviceRequestService.cancelRequest(
                UUID.fromString(id), current.getId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Solicitud cancelada"));
    }
}
