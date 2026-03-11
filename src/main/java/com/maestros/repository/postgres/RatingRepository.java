package com.maestros.repository.postgres;

import com.maestros.base.BaseRepository;
import com.maestros.model.postgres.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RatingRepository extends BaseRepository<Rating> {

    Page<Rating> findByRatedIdOrderByCreatedAtDesc(UUID ratedId, Pageable pageable);

    List<Rating> findTop3ByRatedIdOrderByCreatedAtDesc(UUID ratedId);

    boolean existsByRaterIdAndServiceRequestId(UUID raterId, UUID serviceRequestId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.rated.id = :ratedId")
    Double findAverageScoreByRatedId(@Param("ratedId") UUID ratedId);

    long countByRatedId(UUID ratedId);
}
