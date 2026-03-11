package com.maestros.dto.response;

public record AddressResponse(
        String street,
        String number,
        String city,
        String additionalInstructions) {
}
