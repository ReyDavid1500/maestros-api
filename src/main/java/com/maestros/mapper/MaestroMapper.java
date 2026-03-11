package com.maestros.mapper;

import com.maestros.dto.response.MaestroListItemResponse;
import com.maestros.dto.response.MaestroProfileResponse;
import com.maestros.dto.response.MaestroServiceResponse;
import com.maestros.dto.response.ServiceCategoryResponse;
import com.maestros.model.postgres.MaestroProfile;
import com.maestros.model.postgres.MaestroService;
import com.maestros.model.postgres.ServiceCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MaestroMapper {

    @Mapping(target = "id", expression = "java(profile.getId().toString())")
    @Mapping(target = "userId", expression = "java(profile.getUser().getId().toString())")
    @Mapping(target = "name", expression = "java(profile.getUser().getName())")
    @Mapping(target = "photoUrl", expression = "java(profile.getUser().getPhotoUrl())")
    @Mapping(target = "services", source = "services")
    @Mapping(target = "isAvailable", expression = "java(profile.isAvailable())")
    @Mapping(target = "isVerified", expression = "java(profile.isVerified())")
    @Mapping(target = "recentRatings", ignore = true)
    MaestroProfileResponse toMaestroProfileResponse(MaestroProfile profile);

    @Mapping(target = "id", expression = "java(profile.getId().toString())")
    @Mapping(target = "userId", expression = "java(profile.getUser().getId().toString())")
    @Mapping(target = "name", expression = "java(profile.getUser().getName())")
    @Mapping(target = "photoUrl", expression = "java(profile.getUser().getPhotoUrl())")
    @Mapping(target = "descriptionSnippet", expression = "java(profile.getDescription() != null && profile.getDescription().length() > 150 ? profile.getDescription().substring(0, 150) : profile.getDescription())")
    @Mapping(target = "services", source = "services")
    @Mapping(target = "isAvailable", expression = "java(profile.isAvailable())")
    @Mapping(target = "isVerified", expression = "java(profile.isVerified())")
    MaestroListItemResponse toMaestroListItemResponse(MaestroProfile profile);

    // Used automatically by the list mapping in both methods above
    MaestroServiceResponse toMaestroServiceResponse(MaestroService service);

    @Mapping(target = "id", expression = "java(category.getId().toString())")
    ServiceCategoryResponse toServiceCategoryResponse(ServiceCategory category);
}
