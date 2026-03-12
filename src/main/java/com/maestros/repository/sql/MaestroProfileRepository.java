package com.maestros.repository.sql;

import com.maestros.base.BaseRepository;
import com.maestros.model.sql.MaestroProfile;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MaestroProfileRepository
        extends BaseRepository<MaestroProfile>, JpaSpecificationExecutor<MaestroProfile> {

    Optional<MaestroProfile> findByUserId(UUID userId);

    @Query("""
            SELECT DISTINCT mp FROM MaestroProfile mp
            JOIN FETCH mp.user
            LEFT JOIN FETCH mp.services ms
            LEFT JOIN FETCH ms.serviceCategory
            WHERE mp.id = :id
            """)
    Optional<MaestroProfile> findByIdWithDetails(@Param("id") UUID id);
}
