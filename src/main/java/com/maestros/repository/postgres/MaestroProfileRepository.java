package com.maestros.repository.postgres;

import com.maestros.base.BaseRepository;
import com.maestros.model.postgres.MaestroProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MaestroProfileRepository extends BaseRepository<MaestroProfile> {

    Optional<MaestroProfile> findByUserId(UUID userId);

    @Query("""
            SELECT DISTINCT mp FROM MaestroProfile mp
            JOIN mp.services ms
            WHERE ms.serviceCategory.id = :categoryId
            AND mp.isAvailable = true
            """)
    Page<MaestroProfile> findByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);

    @Query("""
            SELECT mp FROM MaestroProfile mp
            JOIN mp.user u
            WHERE mp.isAvailable = true
            AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(mp.description) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<MaestroProfile> searchByText(@Param("query") String query, Pageable pageable);
}
