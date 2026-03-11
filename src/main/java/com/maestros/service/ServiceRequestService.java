package com.maestros.service;

import com.maestros.dto.request.CreateServiceRequestRequest;
import com.maestros.dto.response.AddressResponse;
import com.maestros.dto.response.ServiceRequestResponse;
import com.maestros.exception.BadRequestException;
import com.maestros.exception.ForbiddenException;
import com.maestros.exception.ResourceNotFoundException;
import com.maestros.mapper.MaestroMapper;
import com.maestros.mapper.UserMapper;
import com.maestros.model.enums.RequestStatus;
import com.maestros.model.enums.UserRole;
import com.maestros.model.postgres.MaestroProfile;
import com.maestros.model.postgres.ServiceRequest;
import com.maestros.model.postgres.User;
import com.maestros.repository.postgres.MaestroProfileRepository;
import com.maestros.repository.postgres.ServiceCategoryRepository;
import com.maestros.repository.postgres.ServiceRequestRepository;
import com.maestros.repository.postgres.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;
    private final MaestroProfileRepository maestroProfileRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserMapper userMapper;
    private final MaestroMapper maestroMapper;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    public ServiceRequestResponse createServiceRequest(UUID clientId, CreateServiceRequestRequest request) {
        User client = userRepository.findById(clientId)
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        Instant scheduledAt;
        try {
            scheduledAt = Instant.parse(request.scheduledAt());
        } catch (Exception e) {
            throw new BadRequestException("El formato de scheduledAt debe ser ISO 8601 (ej: 2025-01-01T10:00:00Z)");
        }
        if (scheduledAt.isBefore(Instant.now().plusSeconds(7200))) {
            throw new BadRequestException("El horario debe ser al menos 2 horas desde ahora");
        }

        UUID maestroProfileId = UUID.fromString(request.maestroId());
        MaestroProfile maestroProfile = maestroProfileRepository.findByIdWithDetails(maestroProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de maestro no encontrado"));

        if (!maestroProfile.isAvailable()) {
            throw new BadRequestException("El maestro no está disponible en este momento");
        }

        UUID categoryId = UUID.fromString(request.serviceCategoryId());
        boolean offersCategory = maestroProfile.getServices().stream()
                .anyMatch(s -> s.getServiceCategory().getId().equals(categoryId));
        if (!offersCategory) {
            throw new BadRequestException("El maestro no ofrece este servicio");
        }

        var serviceCategory = serviceCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        ServiceRequest serviceRequest = ServiceRequest.builder()
                .client(client)
                .maestro(maestroProfile.getUser())
                .serviceCategory(serviceCategory)
                .description(request.description())
                .addressStreet(request.addressStreet())
                .addressNumber(request.addressNumber())
                .addressCity(request.addressCity())
                .addressInstructions(request.addressInstructions())
                .scheduledAt(scheduledAt)
                .build();

        serviceRequest = serviceRequestRepository.save(serviceRequest);

        String requestId = serviceRequest.getId().toString();
        String maestroUserId = maestroProfile.getUser().getId().toString();

        notificationService.notifyRequestCreated(
                maestroProfile.getUser().getFcmToken(), client.getName(), requestId);
        sendWebSocketNotification(maestroUserId, requestId, "CREATED", serviceRequest.getStatus().name());

        return toResponse(serviceRequest, maestroProfile);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> getMyServiceRequests(UUID userId, UserRole role, Pageable pageable) {
        if (role == UserRole.CLIENT) {
            return serviceRequestRepository.findByClientId(userId, pageable)
                    .map(sr -> toResponse(sr, sr.getMaestro().getMaestroProfile()));
        } else {
            return serviceRequestRepository.findByMaestroId(userId, pageable)
                    .map(sr -> toResponse(sr, sr.getMaestro().getMaestroProfile()));
        }
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse getServiceRequestDetail(UUID requestId, UUID userId) {
        ServiceRequest sr = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));

        boolean isClient = sr.getClient().getId().equals(userId);
        boolean isMaestro = sr.getMaestro().getId().equals(userId);
        if (!isClient && !isMaestro) {
            throw new ForbiddenException("No tienes acceso a esta solicitud");
        }

        return toResponse(sr, sr.getMaestro().getMaestroProfile());
    }

    // -------------------------------------------------------------------------
    // State machine transitions
    // -------------------------------------------------------------------------

    public ServiceRequestResponse acceptRequest(UUID requestId, UUID maestroUserId) {
        ServiceRequest sr = loadAndVerifyMaestro(requestId, maestroUserId);

        if (sr.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("La solicitud no está en estado PENDIENTE");
        }
        sr.setStatus(RequestStatus.ACCEPTED);
        sr.setAcceptedAt(Instant.now());
        sr = serviceRequestRepository.save(sr);

        String srId = sr.getId().toString();
        MaestroProfile maestroProfile = sr.getMaestro().getMaestroProfile();
        String maestroName = sr.getMaestro().getName();

        notificationService.notifyRequestAccepted(sr.getClient().getFcmToken(), maestroName, srId);
        sendWebSocketNotification(sr.getClient().getId().toString(), srId, "ACCEPTED", sr.getStatus().name());

        return toResponse(sr, maestroProfile);
    }

    public ServiceRequestResponse rejectRequest(UUID requestId, UUID maestroUserId) {
        ServiceRequest sr = loadAndVerifyMaestro(requestId, maestroUserId);

        if (sr.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("La solicitud no está en estado PENDIENTE");
        }
        sr.setStatus(RequestStatus.CANCELLED);
        sr.setCancelledAt(Instant.now());
        sr = serviceRequestRepository.save(sr);

        String srId = sr.getId().toString();
        MaestroProfile maestroProfile = sr.getMaestro().getMaestroProfile();
        String maestroName = sr.getMaestro().getName();

        notificationService.notifyRequestRejected(sr.getClient().getFcmToken(), maestroName, srId);
        sendWebSocketNotification(sr.getClient().getId().toString(), srId, "REJECTED", sr.getStatus().name());

        return toResponse(sr, maestroProfile);
    }

    public ServiceRequestResponse startWork(UUID requestId, UUID maestroUserId) {
        ServiceRequest sr = loadAndVerifyMaestro(requestId, maestroUserId);

        if (sr.getStatus() != RequestStatus.ACCEPTED) {
            throw new BadRequestException("La solicitud no está en estado ACEPTADO");
        }
        sr.setStatus(RequestStatus.IN_PROGRESS);
        sr.setStartedAt(Instant.now());
        sr = serviceRequestRepository.save(sr);

        String srId = sr.getId().toString();
        MaestroProfile maestroProfile = sr.getMaestro().getMaestroProfile();
        String maestroName = sr.getMaestro().getName();

        notificationService.notifyWorkStarted(sr.getClient().getFcmToken(), maestroName, srId);
        sendWebSocketNotification(sr.getClient().getId().toString(), srId, "STARTED", sr.getStatus().name());

        return toResponse(sr, maestroProfile);
    }

    public ServiceRequestResponse completeWork(UUID requestId, UUID maestroUserId) {
        ServiceRequest sr = loadAndVerifyMaestro(requestId, maestroUserId);

        if (sr.getStatus() != RequestStatus.IN_PROGRESS) {
            throw new BadRequestException("La solicitud no está en estado EN PROGRESO");
        }
        sr.setStatus(RequestStatus.COMPLETED);
        sr.setCompletedAt(Instant.now());
        sr = serviceRequestRepository.save(sr);

        MaestroProfile maestroProfile = sr.getMaestro().getMaestroProfile();
        if (maestroProfile != null) {
            maestroProfile.setTotalJobs(maestroProfile.getTotalJobs() + 1);
            maestroProfileRepository.save(maestroProfile);
        }

        String srId = sr.getId().toString();
        String maestroName = sr.getMaestro().getName();

        notificationService.notifyWorkCompleted(sr.getClient().getFcmToken(), maestroName, srId);
        sendWebSocketNotification(sr.getClient().getId().toString(), srId, "COMPLETED", sr.getStatus().name());

        return toResponse(sr, maestroProfile);
    }

    public ServiceRequestResponse cancelRequest(UUID requestId, UUID clientId) {
        ServiceRequest sr = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));

        if (!sr.getClient().getId().equals(clientId)) {
            throw new ForbiddenException("No tienes acceso a esta solicitud");
        }
        if (sr.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Solo se pueden cancelar solicitudes en estado PENDIENTE");
        }
        sr.setStatus(RequestStatus.CANCELLED);
        sr.setCancelledAt(Instant.now());
        sr = serviceRequestRepository.save(sr);

        String srId = sr.getId().toString();
        MaestroProfile maestroProfile = sr.getMaestro().getMaestroProfile();
        String maestroUserId = sr.getMaestro().getId().toString();
        String clientName = sr.getClient().getName();

        notificationService.notifyRequestCancelled(sr.getMaestro().getFcmToken(), clientName, srId);
        sendWebSocketNotification(maestroUserId, srId, "CANCELLED", sr.getStatus().name());

        return toResponse(sr, maestroProfile);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private ServiceRequest loadAndVerifyMaestro(UUID requestId, UUID maestroUserId) {
        ServiceRequest sr = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
        if (!sr.getMaestro().getId().equals(maestroUserId)) {
            throw new ForbiddenException("No tienes acceso a esta solicitud");
        }
        return sr;
    }

    private ServiceRequestResponse toResponse(ServiceRequest sr, MaestroProfile maestroProfile) {
        AddressResponse address = new AddressResponse(
                sr.getAddressStreet(),
                sr.getAddressNumber(),
                sr.getAddressCity(),
                sr.getAddressInstructions());

        return new ServiceRequestResponse(
                sr.getId().toString(),
                userMapper.toUserResponse(sr.getClient()),
                maestroProfile != null ? maestroMapper.toMaestroProfileResponse(maestroProfile) : null,
                maestroMapper.toServiceCategoryResponse(sr.getServiceCategory()),
                sr.getDescription(),
                address,
                sr.getScheduledAt().toString(),
                sr.getPaymentMethod().name(),
                sr.getStatus().name(),
                sr.getAcceptedAt() != null ? sr.getAcceptedAt().toString() : null,
                sr.getStartedAt() != null ? sr.getStartedAt().toString() : null,
                sr.getCompletedAt() != null ? sr.getCompletedAt().toString() : null,
                sr.getCancelledAt() != null ? sr.getCancelledAt().toString() : null,
                sr.getCreatedAt().toString());
    }

    private void sendWebSocketNotification(String userId, String serviceRequestId, String action, String status) {
        Map<String, String> payload = Map.of(
                "type", "SERVICE_REQUEST",
                "serviceRequestId", serviceRequestId,
                "action", action,
                "status", status);
        try {
            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", payload);
        } catch (Exception e) {
            log.warn("WebSocket notification could not be sent to user [{}]: {}", userId, e.getMessage());
        }
    }
}
