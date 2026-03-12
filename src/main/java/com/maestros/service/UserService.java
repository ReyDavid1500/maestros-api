package com.maestros.service;

import com.maestros.dto.request.UpdateUserRequest;
import com.maestros.dto.response.UserResponse;
import com.maestros.exception.BadRequestException;
import com.maestros.exception.ForbiddenException;
import com.maestros.exception.ResourceNotFoundException;
import com.maestros.mapper.UserMapper;
import com.maestros.model.enums.RequestStatus;
import com.maestros.model.enums.UserRole;
import com.maestros.model.sql.MaestroProfile;
import com.maestros.model.sql.User;
import com.maestros.repository.sql.ServiceRequestRepository;
import com.maestros.repository.sql.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final UserMapper userMapper;

    @Value("${app.azure.storage-base-url:}")
    private String azureStorageBaseUrl;

    public UserService(UserRepository userRepository,
            ServiceRequestRepository serviceRequestRepository,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.serviceRequestRepository = serviceRequestRepository;
        this.userMapper = userMapper;
    }

    public UserResponse getMyProfile(UUID userId) {
        User user = loadActiveUser(userId);
        return buildUserResponse(user);
    }

    public UserResponse updateMyProfile(UUID userId, UpdateUserRequest request) {
        User user = loadActiveUser(userId);

        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.photoUrl() != null) {
            if (azureStorageBaseUrl.isBlank() || !request.photoUrl().startsWith(azureStorageBaseUrl)) {
                throw new BadRequestException("URL de imagen no permitida");
            }
            user.setPhotoUrl(request.photoUrl());
        }

        user = userRepository.save(user);
        return buildUserResponse(user);
    }

    public void updateFcmToken(UUID userId, String fcmToken) {
        User user = loadActiveUser(userId);
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    public void deactivateMyAccount(UUID userId) {
        User user = loadActiveUser(userId);

        List<RequestStatus> activeStatuses = List.of(
                RequestStatus.PENDING, RequestStatus.ACCEPTED, RequestStatus.IN_PROGRESS);

        boolean hasActiveRequests = serviceRequestRepository.existsByClientIdAndStatusIn(userId, activeStatuses)
                || serviceRequestRepository.existsByMaestroIdAndStatusIn(userId, activeStatuses);

        if (hasActiveRequests) {
            throw new BadRequestException(
                    "No puedes desactivar tu cuenta mientras tienes solicitudes activas");
        }

        user.setActive(false);
        userRepository.save(user);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User loadActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (!user.isActive()) {
            throw new ForbiddenException("Cuenta desactivada");
        }
        return user;
    }

    private UserResponse buildUserResponse(User user) {
        UserResponse base = userMapper.toUserResponse(user);
        if (user.getRole() == UserRole.MAESTRO && user.getMaestroProfile() != null) {
            MaestroProfile profile = user.getMaestroProfile();
            return new UserResponse(
                    base.id(), base.name(), base.email(), base.photoUrl(),
                    base.phone(), base.role(), base.createdAt(), true,
                    profile.getAverageRating(), profile.getTotalJobs());
        }
        return base;
    }
}
