package com.maestros.service;

import com.maestros.dto.request.CreateRatingRequest;
import com.maestros.dto.response.RatingResponse;
import com.maestros.dto.response.RatingResponse.RaterInfo;
import com.maestros.dto.response.RatingsPageResponse;
import com.maestros.exception.BadRequestException;
import com.maestros.exception.ConflictException;
import com.maestros.exception.ForbiddenException;
import com.maestros.exception.ResourceNotFoundException;
import com.maestros.model.enums.RequestStatus;
import com.maestros.model.postgres.Rating;
import com.maestros.model.postgres.ServiceRequest;
import com.maestros.repository.postgres.MaestroProfileRepository;
import com.maestros.repository.postgres.RatingRepository;
import com.maestros.repository.postgres.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final MaestroProfileRepository maestroProfileRepository;

    public RatingResponse createRating(UUID raterId, CreateRatingRequest request) {
        UUID serviceRequestId = UUID.fromString(request.serviceRequestId());

        ServiceRequest sr = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));

        if (sr.getStatus() != RequestStatus.COMPLETED) {
            throw new BadRequestException("Solo puedes calificar solicitudes completadas");
        }

        UUID clientId = sr.getClient().getId();
        UUID maestroUserId = sr.getMaestro().getId();

        boolean isClient = raterId.equals(clientId);
        boolean isMaestro = raterId.equals(maestroUserId);
        if (!isClient && !isMaestro) {
            throw new ForbiddenException("No tienes acceso a esta solicitud");
        }

        if (ratingRepository.existsByRaterIdAndServiceRequestId(raterId, serviceRequestId)) {
            throw new ConflictException("Ya has calificado esta solicitud");
        }

        UUID ratedId = isClient ? maestroUserId : clientId;

        String sanitizedComment = null;
        if (request.comment() != null) {
            sanitizedComment = Jsoup.clean(request.comment(), Safelist.none()).strip();
            if (sanitizedComment.isBlank()) {
                sanitizedComment = null;
            }
        }

        var rater = isClient ? sr.getClient() : sr.getMaestro();
        var rated = isClient ? sr.getMaestro() : sr.getClient();

        Rating rating = Rating.builder()
                .rater(rater)
                .rated(rated)
                .serviceRequest(sr)
                .score(request.score())
                .comment(sanitizedComment)
                .build();

        rating = ratingRepository.save(rating);

        // Recalculate maestro average rating only when the rated user is the maestro
        if (ratedId.equals(maestroUserId)) {
            maestroProfileRepository.findByUserId(maestroUserId).ifPresent(profile -> {
                Double newAverage = ratingRepository.findAverageScoreByRatedId(maestroUserId);
                profile.setAverageRating(newAverage != null ? newAverage : 0.0);
                maestroProfileRepository.save(profile);
            });
        }

        return toResponse(rating);
    }

    @Transactional(readOnly = true)
    public RatingsPageResponse getMaestroRatings(UUID maestroUserId, Pageable pageable) {
        Page<Rating> page = ratingRepository.findByRatedIdOrderByCreatedAtDesc(maestroUserId, pageable);

        List<RatingResponse> ratings = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        Double averageScore = ratingRepository.findAverageScoreByRatedId(maestroUserId);
        long totalRatings = ratingRepository.countByRatedId(maestroUserId);

        return new RatingsPageResponse(
                ratings,
                averageScore != null ? averageScore : 0.0,
                totalRatings,
                page.getNumber(),
                page.getSize(),
                page.getTotalPages());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private RatingResponse toResponse(Rating rating) {
        var u = rating.getRater();
        return new RatingResponse(
                rating.getId().toString(),
                new RaterInfo(u.getId().toString(), u.getName(), u.getPhotoUrl()),
                rating.getScore(),
                rating.getComment(),
                rating.getCreatedAt().toString());
    }
}
