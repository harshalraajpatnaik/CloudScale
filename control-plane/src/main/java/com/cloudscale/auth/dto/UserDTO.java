package com.cloudscale.auth.dto;

import com.cloudscale.auth.entity.User;
import com.cloudscale.auth.model.Role;
import java.time.Instant;
import java.util.UUID;

public record UserDTO(
        UUID id,
        UUID organisationId,
        String email,
        String fullName,
        Role role,
        boolean active,
        Instant lastLoginAt
) {
    public static UserDTO fromEntity(User user) {
        return new UserDTO(
                user.getId(),
                user.getOrganisation().getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.getLastLoginAt()
        );
    }
}
