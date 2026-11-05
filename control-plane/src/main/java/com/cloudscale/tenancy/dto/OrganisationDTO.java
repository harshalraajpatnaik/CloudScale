package com.cloudscale.tenancy.dto;

import com.cloudscale.tenancy.entity.Organisation;
import java.time.Instant;
import java.util.UUID;

public record OrganisationDTO(
        UUID id,
        String name,
        String slug,
        String planTier,
        int maxCpuQuota,
        long maxMemoryMbQuota,
        Instant createdAt
) {
    public static OrganisationDTO fromEntity(Organisation org) {
        return new OrganisationDTO(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.getPlanTier(),
                org.getMaxCpuQuota(),
                org.getMaxMemoryMbQuota(),
                org.getCreatedAt()
        );
    }
}
