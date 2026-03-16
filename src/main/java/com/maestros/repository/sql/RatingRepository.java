package com.maestros.repository.sql;

import com.maestros.base.BaseRepository;
import com.maestros.model.sql.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RatingRepository extends BaseRepository<Rating> {

    Page<Rating> findByRatedIdOrderByCreatedAtDesc(Long ratedId, Pageable pageable);

    List<Rating> findTop3ByRatedIdOrderByCreatedAtDesc(Long ratedId);

    boolean existsByRaterIdAndServiceRequestId(Long raterId, Long serviceRequestId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.rated.id = :ratedId")
    Double findAverageScoreByRatedId(@Param("ratedId") Long ratedId);

    long countByRatedId(Long ratedId);
}
