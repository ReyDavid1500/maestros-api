package com.maestros.service;

import com.maestros.dto.response.ServiceCategoryResponse;
import com.maestros.mapper.MaestroMapper;
import com.maestros.repository.sql.ServiceCategoryRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final ServiceCategoryRepository serviceCategoryRepository;
    private final MaestroMapper maestroMapper;

    public CategoryService(ServiceCategoryRepository serviceCategoryRepository,
            MaestroMapper maestroMapper) {
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.maestroMapper = maestroMapper;
    }

    @Cacheable("categories")
    public List<ServiceCategoryResponse> getAllCategories() {
        return serviceCategoryRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(maestroMapper::toServiceCategoryResponse)
                .toList();
    }
}
