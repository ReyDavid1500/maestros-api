package com.maestros.service;

import com.maestros.dto.request.CreateMaestroProfileRequest;
import com.maestros.dto.request.MaestroServiceRequest;
import com.maestros.dto.request.UpdateMaestroProfileRequest;
import com.maestros.dto.request.UpdateServicesRequest;
import com.maestros.dto.response.MaestroListItemResponse;
import com.maestros.dto.response.MaestroProfileResponse;
import com.maestros.exception.BadRequestException;
import com.maestros.exception.ConflictException;
import com.maestros.exception.ForbiddenException;
import com.maestros.exception.ResourceNotFoundException;
import com.maestros.mapper.MaestroMapper;
import com.maestros.model.MaestroSearchFilters;
import com.maestros.model.enums.UserRole;
import com.maestros.model.postgres.MaestroProfile;
import com.maestros.model.postgres.User;
import com.maestros.repository.postgres.MaestroProfileRepository;
import com.maestros.repository.postgres.ServiceCategoryRepository;
import com.maestros.repository.postgres.UserRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MaestroService {

    private final MaestroProfileRepository maestroProfileRepository;
    private final UserRepository userRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final MaestroMapper maestroMapper;

    // -------------------------------------------------------------------------
    // Public queries
    // -------------------------------------------------------------------------

    public Page<MaestroListItemResponse> listMaestros(MaestroSearchFilters filters, Pageable pageable) {
        Specification<MaestroProfile> spec = buildListSpec(filters);
        return maestroProfileRepository.findAll(spec, pageable)
                .map(maestroMapper::toMaestroListItemResponse);
    }

    public Page<MaestroListItemResponse> searchMaestros(String query, String categoryIdStr, Pageable pageable) {
        UUID categoryId = categoryIdStr != null ? UUID.fromString(categoryIdStr) : null;
        Specification<MaestroProfile> spec = buildSearchSpec(query, categoryId);
        return maestroProfileRepository.findAll(spec, pageable)
                .map(maestroMapper::toMaestroListItemResponse);
    }

    public MaestroProfileResponse getMaestroDetail(UUID maestroProfileId) {
        Optional<MaestroProfile> profileOpt = maestroProfileRepository.findByIdWithDetails(maestroProfileId);
        if (profileOpt.isEmpty()) {
            // Timing-attack protection: delay to normalise response time and prevent ID
            // enumeration
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new ResourceNotFoundException("Maestro no encontrado");
        }
        return maestroMapper.toMaestroProfileResponse(profileOpt.get());
    }

    // -------------------------------------------------------------------------
    // Protected profile management
    // -------------------------------------------------------------------------

    @Transactional
    public MaestroProfileResponse createMyProfile(UUID userId, CreateMaestroProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (user.getRole() != UserRole.MAESTRO) {
            throw new ForbiddenException("Solo los maestros pueden crear un perfil de maestro");
        }
        if (maestroProfileRepository.findByUserId(userId).isPresent()) {
            throw new ConflictException("Ya tienes un perfil de maestro");
        }

        user.setPhone(request.phone());
        userRepository.save(user);

        MaestroProfile profile = MaestroProfile.builder()
                .user(user)
                .description(request.description())
                .build();

        for (MaestroServiceRequest svcReq : request.services()) {
            UUID catId = UUID.fromString(svcReq.serviceCategoryId());
            com.maestros.model.postgres.ServiceCategory category = serviceCategoryRepository.findById(catId)
                    .orElseThrow(() -> new BadRequestException(
                            "Categoría de servicio no válida: " + svcReq.serviceCategoryId()));
            com.maestros.model.postgres.MaestroService svc = com.maestros.model.postgres.MaestroService.builder()
                    .maestroProfile(profile)
                    .serviceCategory(category)
                    .priceClp(svcReq.priceClp())
                    .estimatedTime(svcReq.estimatedTime())
                    .build();
            profile.getServices().add(svc);
        }

        MaestroProfile saved = maestroProfileRepository.save(profile);
        return maestroMapper.toMaestroProfileResponse(saved);
    }

    @Transactional
    public MaestroProfileResponse updateMyProfile(UUID userId, UpdateMaestroProfileRequest request) {
        MaestroProfile profile = maestroProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de maestro no encontrado"));

        if (request.description() != null) {
            profile.setDescription(request.description());
        }
        if (request.isAvailable() != null) {
            profile.setAvailable(request.isAvailable());
        }
        if (request.phone() != null) {
            User user = profile.getUser();
            user.setPhone(request.phone());
            userRepository.save(user);
        }

        MaestroProfile saved = maestroProfileRepository.save(profile);
        return maestroMapper.toMaestroProfileResponse(saved);
    }

    @Transactional
    public MaestroProfileResponse updateMyServices(UUID userId, UpdateServicesRequest request) {
        MaestroProfile profile = maestroProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de maestro no encontrado"));

        // Validate no duplicate categories in the incoming list before touching DB
        Set<String> seen = new HashSet<>();
        for (MaestroServiceRequest svcReq : request.services()) {
            if (!seen.add(svcReq.serviceCategoryId())) {
                throw new BadRequestException(
                        "Categoría duplicada en la lista de servicios: " + svcReq.serviceCategoryId());
            }
        }

        // Replace all — orphanRemoval=true on the mapping handles deletes
        profile.getServices().clear();

        for (MaestroServiceRequest svcReq : request.services()) {
            UUID catId = UUID.fromString(svcReq.serviceCategoryId());
            com.maestros.model.postgres.ServiceCategory category = serviceCategoryRepository.findById(catId)
                    .orElseThrow(() -> new BadRequestException(
                            "Categoría de servicio no válida: " + svcReq.serviceCategoryId()));
            com.maestros.model.postgres.MaestroService svc = com.maestros.model.postgres.MaestroService.builder()
                    .maestroProfile(profile)
                    .serviceCategory(category)
                    .priceClp(svcReq.priceClp())
                    .estimatedTime(svcReq.estimatedTime())
                    .build();
            profile.getServices().add(svc);
        }

        MaestroProfile saved = maestroProfileRepository.save(profile);
        return maestroMapper.toMaestroProfileResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Specification builders
    // -------------------------------------------------------------------------

    private Specification<MaestroProfile> buildListSpec(MaestroSearchFilters filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always join user and enforce active = true
            var userJoin = root.join("user", JoinType.INNER);
            predicates.add(cb.isTrue(userJoin.get("active")));

            // city → partial match on description (until explicit city column is added)
            if (filters.getCity() != null && !filters.getCity().isBlank()) {
                String cityPattern = "%" + filters.getCity().toLowerCase() + "%";
                predicates.add(cb.like(
                        cb.lower(cb.coalesce(root.get("description"), cb.literal(""))),
                        cityPattern));
            }

            // minRating → averageRating >= minRating
            if (filters.getMinRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"), filters.getMinRating()));
            }

            // categoryId → EXISTS (SELECT 1 FROM maestro_services ms WHERE
            // ms.maestro_profile_id = mp.id AND ms.service_category_id = :id)
            if (filters.getCategoryId() != null) {
                Subquery<UUID> catSub = query.subquery(UUID.class);
                Root<com.maestros.model.postgres.MaestroService> svcRoot = catSub
                        .from(com.maestros.model.postgres.MaestroService.class);
                catSub.select(svcRoot.get("maestroProfile").get("id"))
                        .where(cb.and(
                                cb.equal(svcRoot.get("maestroProfile").get("id"), root.get("id")),
                                cb.equal(svcRoot.get("serviceCategory").get("id"), filters.getCategoryId())));
                predicates.add(cb.exists(catSub));
            }

            // maxPriceClp → at least one service with priceClp <= maxPriceClp
            if (filters.getMaxPriceClp() != null) {
                Subquery<UUID> priceSub = query.subquery(UUID.class);
                Root<com.maestros.model.postgres.MaestroService> svcRoot = priceSub
                        .from(com.maestros.model.postgres.MaestroService.class);
                priceSub.select(svcRoot.get("maestroProfile").get("id"))
                        .where(cb.and(
                                cb.equal(svcRoot.get("maestroProfile").get("id"), root.get("id")),
                                cb.lessThanOrEqualTo(svcRoot.get("priceClp"), filters.getMaxPriceClp())));
                predicates.add(cb.exists(priceSub));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<MaestroProfile> buildSearchSpec(String query, UUID categoryId) {
        return (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            var userJoin = root.join("user", JoinType.INNER);
            predicates.add(cb.isTrue(userJoin.get("active")));

            // Full-text search: ILIKE on user.name OR description
            String pattern = "%" + query.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(userJoin.get("name")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("description"), cb.literal(""))), pattern)));

            // Optional category filter applied as EXISTS subquery (avoids result
            // duplication)
            if (categoryId != null) {
                Subquery<UUID> catSub = q.subquery(UUID.class);
                Root<com.maestros.model.postgres.MaestroService> svcRoot = catSub
                        .from(com.maestros.model.postgres.MaestroService.class);
                catSub.select(svcRoot.get("maestroProfile").get("id"))
                        .where(cb.and(
                                cb.equal(svcRoot.get("maestroProfile").get("id"), root.get("id")),
                                cb.equal(svcRoot.get("serviceCategory").get("id"), categoryId)));
                predicates.add(cb.exists(catSub));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
