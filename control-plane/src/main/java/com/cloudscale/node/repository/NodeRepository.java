package com.cloudscale.node.repository;

import com.cloudscale.node.entity.Node;
import com.cloudscale.node.model.NodeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NodeRepository extends JpaRepository<Node, UUID> {

    List<Node> findByOrganisationId(UUID organisationId);

    Optional<Node> findByIdAndOrganisationId(UUID id, UUID organisationId);

    Optional<Node> findByHostname(String hostname);

    boolean existsByHostname(String hostname);

    List<Node> findByOrganisationIdAndStatus(UUID organisationId, NodeStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM Node n WHERE n.organisation.id = :orgId AND n.status = 'ACTIVE' ORDER BY n.createdAt ASC")
    List<Node> findActiveNodesForSchedulingWithLock(@Param("orgId") UUID orgId);

    @Query("SELECT n FROM Node n WHERE n.status = 'ACTIVE' AND n.lastHeartbeat < :threshold")
    List<Node> findStaleActiveNodes(@Param("threshold") Instant threshold);
}
