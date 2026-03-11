package com.maestros.dto.request;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateUserRequest(
        @Size(max = 100) String name,
        @Size(max = 20) String phone,
        @URL @Size(max = 500) String photoUrl) {
}
