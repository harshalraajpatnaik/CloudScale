package com.cloudscale.autoscaling.repository;

import com.cloudscale.autoscaling.entity.ScalingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScalingEventRepository extends JpaRepository<ScalingEvent, UUID> {

    @Query("SELECT e FROM ScalingEvent e WHERE e.workload.id = :workloadId ORDER BY e.executedAt DESC")
    List<ScalingEvent> findByWorkloadIdOrderByExecutedAtDesc(@Param("workloadId") UUID workloadId);

    @Query("SELECT e FROM ScalingEvent e WHERE e.scalingPolicy.id = :policyId ORDER BY e.executedAt DESC")
    List<ScalingEvent> findByPolicyIdOrderByExecutedAtDesc(@Param("policyId") UUID policyId);

    @Query("SELECT e FROM ScalingEvent e WHERE e.scalingPolicy.organisation.id = :orgId ORDER BY e.executedAt DESC")
    List<ScalingEvent> findByOrganisationIdOrderByExecutedAtDesc(@Param("orgId") UUID orgId);
}
