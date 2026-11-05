package com.cloudscale.workload.repository;

import com.cloudscale.workload.entity.Workload;
import com.cloudscale.workload.model.WorkloadState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkloadRepository extends JpaRepository<Workload, UUID> {

    List<Workload> findByOrganisationId(UUID organisationId);

    Optional<Workload> findByIdAndOrganisationId(UUID id, UUID organisationId);

    List<Workload> findByOrganisationIdAndActualState(UUID organisationId, WorkloadState actualState);

    List<Workload> findByAssignedNodeId(UUID assignedNodeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Workload w WHERE w.id = :id AND w.organisation.id = :orgId")
    Optional<Workload> findByIdAndOrganisationIdForUpdate(@Param("id") UUID id, @Param("orgId") UUID orgId);
}
