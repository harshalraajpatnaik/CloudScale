package com.cloudscale.auth.dto;

import com.cloudscale.tenancy.dto.OrganisationDTO;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        UserDTO user,
        OrganisationDTO organisation
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresInMs, UserDTO user, OrganisationDTO organisation) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInMs, user, organisation);
    }
}
