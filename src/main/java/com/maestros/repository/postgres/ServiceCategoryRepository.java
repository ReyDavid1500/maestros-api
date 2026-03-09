package com.maestros.repository.postgres;

import com.maestros.base.BaseRepository;
import com.maestros.model.postgres.ServiceCategory;

import java.util.List;

public interface ServiceCategoryRepository extends BaseRepository<ServiceCategory> {

    List<ServiceCategory> findAllByOrderByDisplayOrderAsc();
}
