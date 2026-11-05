package com.cloudscale.autoscaling.repository;

import com.cloudscale.autoscaling.entity.ScalingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScalingPolicyRepository extends JpaRepository<ScalingPolicy, UUID> {

    List<ScalingPolicy> findAllByOrganisationId(UUID organisationId);

    Optional<ScalingPolicy> findByIdAndOrganisationId(UUID id, UUID organisationId);

    Optional<ScalingPolicy> findByWorkloadId(UUID workloadId);

    @Query("SELECT p FROM ScalingPolicy p WHERE p.enabled = true")
    List<ScalingPolicy> findAllActivePolicies();

    @Query("SELECT p FROM ScalingPolicy p WHERE p.organisation.id = :orgId AND p.enabled = true")
    List<ScalingPolicy> findAllActivePoliciesByOrganisationId(@Param("orgId") UUID orgId);
}
