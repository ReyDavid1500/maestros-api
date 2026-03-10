package com.maestros.security;

/**
 * Internal DTO carrying the user profile extracted from a verified Google ID token.
 * Only used within the security layer — never exposed directly in API responses.
 */
public record GoogleUserInfo(String email, String name, String pictureUrl) {}
