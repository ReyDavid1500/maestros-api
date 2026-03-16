package com.maestros.repository.sql;

import com.maestros.base.BaseRepository;
import com.maestros.model.enums.RequestStatus;
import com.maestros.model.sql.ServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServiceRequestRepository extends BaseRepository<ServiceRequest> {

    Page<ServiceRequest> findByClientId(Long clientId, Pageable pageable);

    Page<ServiceRequest> findByMaestroId(Long maestroId, Pageable pageable);

    Optional<ServiceRequest> findByIdAndClientId(Long id, Long clientId);

    Optional<ServiceRequest> findByIdAndMaestroId(Long id, Long maestroId);

    boolean existsByClientIdAndStatusIn(Long clientId, Collection<RequestStatus> statuses);

    boolean existsByMaestroIdAndStatusIn(Long maestroId, Collection<RequestStatus> statuses);

    List<ServiceRequest> findByClientIdOrMaestroId(Long clientId, Long maestroId);
}
