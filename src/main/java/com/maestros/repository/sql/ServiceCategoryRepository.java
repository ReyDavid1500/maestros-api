package com.maestros.repository.sql;

import com.maestros.base.BaseRepository;
import com.maestros.model.sql.ServiceCategory;

import java.util.List;

public interface ServiceCategoryRepository extends BaseRepository<ServiceCategory> {

    List<ServiceCategory> findAllByOrderByDisplayOrderAsc();
}
