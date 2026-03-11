package com.maestros.repository.postgres;

import com.maestros.base.BaseRepository;
import com.maestros.model.enums.RequestStatus;
import com.maestros.model.postgres.ServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRequestRepository extends BaseRepository<ServiceRequest> {

    Page<ServiceRequest> findByClientId(UUID clientId, Pageable pageable);

    Page<ServiceRequest> findByMaestroId(UUID maestroId, Pageable pageable);

    Optional<ServiceRequest> findByIdAndClientId(UUID id, UUID clientId);

    Optional<ServiceRequest> findByIdAndMaestroId(UUID id, UUID maestroId);

    boolean existsByClientIdAndStatusIn(UUID clientId, Collection<RequestStatus> statuses);

    boolean existsByMaestroIdAndStatusIn(UUID maestroId, Collection<RequestStatus> statuses);

    List<ServiceRequest> findByClientIdOrMaestroId(UUID clientId, UUID maestroId);
}
