package com.cloudscale.metrics.repository;

import com.cloudscale.metrics.entity.NodeMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NodeMetricRepository extends JpaRepository<NodeMetric, UUID> {

    @Query("SELECT m FROM NodeMetric m WHERE m.node.id = :nodeId AND m.sampledAt >= :since ORDER BY m.sampledAt ASC")
    List<NodeMetric> findByNodeIdSince(@Param("nodeId") UUID nodeId, @Param("since") Instant since);

    @Query("SELECT m FROM NodeMetric m WHERE m.node.id = :nodeId ORDER BY m.sampledAt DESC LIMIT 1")
    Optional<NodeMetric> findLatestByNodeId(@Param("nodeId") UUID nodeId);

    @Query("SELECT m FROM NodeMetric m WHERE m.node.organisation.id = :orgId AND m.sampledAt >= :since ORDER BY m.sampledAt ASC")
    List<NodeMetric> findByOrganisationIdSince(@Param("orgId") UUID orgId, @Param("since") Instant since);

    @Modifying
    @Query("DELETE FROM NodeMetric m WHERE m.sampledAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
