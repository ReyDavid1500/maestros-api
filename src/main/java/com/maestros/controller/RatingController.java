package com.maestros.controller;

import com.maestros.dto.request.CreateRatingRequest;
import com.maestros.dto.response.ApiResponse;
import com.maestros.dto.response.RatingResponse;
import com.maestros.dto.response.RatingsPageResponse;
import com.maestros.model.postgres.User;
import com.maestros.security.AuthHelper;
import com.maestros.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;
    private final AuthHelper authHelper;

    @PostMapping
    public ResponseEntity<ApiResponse<RatingResponse>> createRating(
            @Valid @RequestBody CreateRatingRequest request) {
        User current = authHelper.getCurrentUser();
        RatingResponse response = ratingService.createRating(current.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Calificación enviada"));
    }

    @GetMapping("/maestro/{maestroId}")
    public ResponseEntity<ApiResponse<RatingsPageResponse>> getMaestroRatings(
            @PathVariable String maestroId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 20));
        RatingsPageResponse response = ratingService.getMaestroRatings(UUID.fromString(maestroId), pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
