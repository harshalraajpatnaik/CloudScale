package com.cloudscale.ai.dto;

import com.cloudscale.ai.entity.AIRecommendation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AIRecommendationDTO(
        UUID id,
        UUID organisationId,
        String category,
        String explanation,
        BigDecimal confidenceScore,
        String expectedImpact,
        String suggestedAction,
        String approvalStatus,
        Instant evaluatedAt
) {
    public static AIRecommendationDTO fromEntity(AIRecommendation r) {
        return new AIRecommendationDTO(
                r.getId(),
                r.getOrganisation().getId(),
                r.getCategory(),
                r.getExplanation(),
                r.getConfidenceScore(),
                r.getExpectedImpact(),
                r.getSuggestedAction(),
                r.getApprovalStatus(),
                r.getEvaluatedAt()
        );
    }
}
