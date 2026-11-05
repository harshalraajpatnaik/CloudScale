package com.cloudscale.alert.repository;

import com.cloudscale.alert.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findByOrganisationIdOrderByTriggeredAtDesc(UUID orgId);

    List<Alert> findByOrganisationIdAndStatusOrderByTriggeredAtDesc(UUID orgId, String status);

    Optional<Alert> findByIdAndOrganisationId(UUID id, UUID orgId);

    long countByOrganisationIdAndStatus(UUID orgId, String status);
}
