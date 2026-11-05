package com.cloudscale.ai.repository;

import com.cloudscale.ai.entity.AIRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AIRecommendationRepository extends JpaRepository<AIRecommendation, UUID> {

    List<AIRecommendation> findAllByOrganisationIdOrderByEvaluatedAtDesc(UUID organisationId);

    List<AIRecommendation> findAllByOrganisationIdAndApprovalStatusOrderByEvaluatedAtDesc(UUID organisationId, String approvalStatus);

    Optional<AIRecommendation> findByIdAndOrganisationId(UUID id, UUID organisationId);
}
