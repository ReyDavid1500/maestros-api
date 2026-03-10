package com.maestros.mapper;

import com.maestros.dto.response.UserResponse;
import com.maestros.model.postgres.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", expression = "java(user.getId().toString())")
    @Mapping(target = "role", expression = "java(user.getRole().name())")
    @Mapping(target = "createdAt", expression = "java(user.getCreatedAt().toString())")
    @Mapping(target = "hasMaestroProfile", expression = "java(user.getMaestroProfile() != null)")
    UserResponse toUserResponse(User user);
}
