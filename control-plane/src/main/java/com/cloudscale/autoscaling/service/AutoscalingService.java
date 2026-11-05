package com.cloudscale.autoscaling.service;

import com.cloudscale.autoscaling.dto.CreateScalingPolicyRequest;
import com.cloudscale.autoscaling.dto.ScalingEvaluationResult;
import com.cloudscale.autoscaling.dto.ScalingEventDTO;
import com.cloudscale.autoscaling.dto.ScalingPolicyDTO;
import com.cloudscale.autoscaling.entity.ScalingEvent;
import com.cloudscale.autoscaling.entity.ScalingPolicy;
import com.cloudscale.autoscaling.repository.ScalingEventRepository;
import com.cloudscale.autoscaling.repository.ScalingPolicyRepository;
import com.cloudscale.common.exception.ConflictException;
import com.cloudscale.common.exception.ResourceNotFoundException;
import com.cloudscale.metrics.entity.NodeMetric;
import com.cloudscale.metrics.repository.NodeMetricRepository;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import com.cloudscale.workload.entity.Workload;
import com.cloudscale.workload.model.WorkloadState;
import com.cloudscale.workload.repository.WorkloadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AutoscalingService {

    private static final Logger log = LoggerFactory.getLogger(AutoscalingService.class);

    private final ScalingPolicyRepository policyRepository;
    private final ScalingEventRepository eventRepository;
    private final WorkloadRepository workloadRepository;
    private final OrganisationRepository organisationRepository;
    private final NodeMetricRepository nodeMetricRepository;

    public AutoscalingService(
            ScalingPolicyRepository policyRepository,
            ScalingEventRepository eventRepository,
            WorkloadRepository workloadRepository,
            OrganisationRepository organisationRepository,
            NodeMetricRepository nodeMetricRepository
    ) {
        this.policyRepository = policyRepository;
        this.eventRepository = eventRepository;
        this.workloadRepository = workloadRepository;
        this.organisationRepository = organisationRepository;
        this.nodeMetricRepository = nodeMetricRepository;
    }

    @Transactional
    public ScalingPolicyDTO createPolicy(UUID orgId, CreateScalingPolicyRequest req) {
        Organisation org = organisationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ORG_NOT_FOUND", "Organisation not found"));

        Workload workload = workloadRepository.findById(req.workloadId())
                .filter(w -> w.getOrganisation().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("WORKLOAD_NOT_FOUND", "Workload not found in organisation"));

        if (policyRepository.findByWorkloadId(workload.getId()).isPresent()) {
            throw new ConflictException("POLICY_EXISTS", "A scaling policy already exists for workload " + workload.getName());
        }

        if (req.cpuScaleUpThreshold().compareTo(req.cpuScaleDownThreshold()) <= 0) {
            throw new ConflictException("INVALID_THRESHOLDS", "Scale-up threshold must be strictly greater than scale-down threshold");
        }

        if (req.maxReplicas() < req.minReplicas()) {
            throw new ConflictException("INVALID_REPLICAS", "Max replicas must be greater than or equal to min replicas");
        }

        ScalingPolicy policy = new ScalingPolicy(
                org,
                workload,
                req.cpuScaleUpThreshold(),
                req.cpuScaleDownThreshold(),
                req.memoryScaleUpThreshold() != null ? req.memoryScaleUpThreshold() : new BigDecimal("85.00"),
                req.memoryScaleDownThreshold() != null ? req.memoryScaleDownThreshold() : new BigDecimal("35.00"),
                req.minReplicas(),
                req.maxReplicas(),
                req.cooldownSeconds(),
                req.evaluationWindowSeconds()
        );

        ScalingPolicy saved = policyRepository.save(policy);
        log.info("Created autoscaling policy for workload {} [Scale-up: {}%, Scale-down: {}%]",
                workload.getName(), saved.getCpuScaleUpThreshold(), saved.getCpuScaleDownThreshold());
        return ScalingPolicyDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ScalingPolicyDTO> listPolicies(UUID orgId) {
        return policyRepository.findAllByOrganisationId(orgId).stream()
                .map(ScalingPolicyDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScalingPolicyDTO getPolicy(UUID orgId, UUID policyId) {
        ScalingPolicy policy = policyRepository.findByIdAndOrganisationId(policyId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("POLICY_NOT_FOUND", "Scaling policy not found"));
        return ScalingPolicyDTO.fromEntity(policy);
    }

    @Transactional
    public void deletePolicy(UUID orgId, UUID policyId) {
        ScalingPolicy policy = policyRepository.findByIdAndOrganisationId(policyId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("POLICY_NOT_FOUND", "Scaling policy not found"));
        policyRepository.delete(policy);
        log.info("Deleted scaling policy {}", policyId);
    }

    @Transactional
    public ScalingEvaluationResult evaluatePolicy(UUID orgId, UUID policyId, BigDecimal simulatedCpuUtil) {
        ScalingPolicy policy = policyRepository.findByIdAndOrganisationId(policyId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("POLICY_NOT_FOUND", "Scaling policy not found"));

        return performEvaluation(policy, simulatedCpuUtil);
    }

    @Transactional
    public ScalingEvaluationResult performEvaluation(ScalingPolicy policy, BigDecimal overrideCpuUtil) {
        Instant now = Instant.now();
        Workload workload = policy.getWorkload();

        // 1. Cooldown enforcement (Oscillation Damping)
        if (policy.isInCooldown(now)) {
            long remainingCooldown = ChronoUnit.SECONDS.between(now, policy.getLastScaledAt().plusSeconds(policy.getCooldownSeconds()));
            log.debug("Policy {} for workload {} is in cooldown ({}s remaining)",
                    policy.getId(), workload.getName(), remainingCooldown);
            return new ScalingEvaluationResult(
                    policy.getId(),
                    workload.getId(),
                    "COOLDOWN_ACTIVE",
                    1,
                    1,
                    "COOLDOWN",
                    BigDecimal.ZERO,
                    "Cooldown period active (" + remainingCooldown + "s remaining). Scaling suppressed to prevent oscillation."
            );
        }

        // 2. Determine metric value (either passed as override/test or queried from NodeMetric)
        BigDecimal currentCpu = overrideCpuUtil;
        if (currentCpu == null) {
            if (workload.getAssignedNode() != null) {
                Instant since = now.minus(policy.getEvaluationWindowSeconds(), ChronoUnit.SECONDS);
                List<NodeMetric> metrics = nodeMetricRepository.findByNodeIdSince(workload.getAssignedNode().getId(), since);
                if (!metrics.isEmpty()) {
                    BigDecimal sum = metrics.stream()
                            .map(NodeMetric::getCpuUtilizationPercent)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    currentCpu = sum.divide(BigDecimal.valueOf(metrics.size()), 2, RoundingMode.HALF_UP);
                }
            }
        }

        if (currentCpu == null) {
            currentCpu = BigDecimal.ZERO;
        }

        int currentReplicas = 1; // Base replica count for single workload instance

        // 3. Evaluation decision logic
        if (currentCpu.compareTo(policy.getCpuScaleUpThreshold()) >= 0) {
            if (currentReplicas >= policy.getMaxReplicas()) {
                return new ScalingEvaluationResult(
                        policy.getId(),
                        workload.getId(),
                        "CLAMPED_AT_LIMIT",
                        currentReplicas,
                        currentReplicas,
                        "CPU_UTILIZATION",
                        currentCpu,
                        "CPU saturation at " + currentCpu + "% exceeds threshold of " + policy.getCpuScaleUpThreshold() + "%, but already at max replicas (" + policy.getMaxReplicas() + ")."
                );
            }

            int newReplicas = Math.min(policy.getMaxReplicas(), currentReplicas + 1);
            policy.setLastScaledAt(now);
            policyRepository.save(policy);

            ScalingEvent event = new ScalingEvent(
                    policy,
                    workload,
                    "SCALE_UP",
                    currentReplicas,
                    newReplicas,
                    "CPU_UTILIZATION",
                    currentCpu,
                    "CPU utilization of " + currentCpu + "% exceeded scale-up threshold (" + policy.getCpuScaleUpThreshold() + "%). Scaled up to " + newReplicas + " replicas."
            );
            eventRepository.save(event);

            log.info("AUTOSCALING: SCALE_UP for workload {} -> {} replicas (CPU: {}%)", workload.getName(), newReplicas, currentCpu);

            return new ScalingEvaluationResult(
                    policy.getId(),
                    workload.getId(),
                    "SCALE_UP",
                    currentReplicas,
                    newReplicas,
                    "CPU_UTILIZATION",
                    currentCpu,
                    event.getReasoning()
            );

        } else if (currentCpu.compareTo(policy.getCpuScaleDownThreshold()) <= 0) {
            if (currentReplicas <= policy.getMinReplicas()) {
                return new ScalingEvaluationResult(
                        policy.getId(),
                        workload.getId(),
                        "CLAMPED_AT_LIMIT",
                        currentReplicas,
                        currentReplicas,
                        "CPU_UTILIZATION",
                        currentCpu,
                        "CPU saturation at " + currentCpu + "% is below scale-down threshold (" + policy.getCpuScaleDownThreshold() + "%), but already at min replicas (" + policy.getMinReplicas() + ")."
                );
            }

            int newReplicas = Math.max(policy.getMinReplicas(), currentReplicas - 1);
            policy.setLastScaledAt(now);
            policyRepository.save(policy);

            ScalingEvent event = new ScalingEvent(
                    policy,
                    workload,
                    "SCALE_DOWN",
                    currentReplicas,
                    newReplicas,
                    "CPU_UTILIZATION",
                    currentCpu,
                    "CPU utilization of " + currentCpu + "% fell below scale-down threshold (" + policy.getCpuScaleDownThreshold() + "%). Scaled down to " + newReplicas + " replicas."
            );
            eventRepository.save(event);

            log.info("AUTOSCALING: SCALE_DOWN for workload {} -> {} replicas (CPU: {}%)", workload.getName(), newReplicas, currentCpu);

            return new ScalingEvaluationResult(
                    policy.getId(),
                    workload.getId(),
                    "SCALE_DOWN",
                    currentReplicas,
                    newReplicas,
                    "CPU_UTILIZATION",
                    currentCpu,
                    event.getReasoning()
            );

        } else {
            return new ScalingEvaluationResult(
                    policy.getId(),
                    workload.getId(),
                    "NOOP_WITHIN_BOUNDS",
                    currentReplicas,
                    currentReplicas,
                    "CPU_UTILIZATION",
                    currentCpu,
                    "CPU utilization of " + currentCpu + "% is healthy within operational thresholds [" + policy.getCpuScaleDownThreshold() + "% - " + policy.getCpuScaleUpThreshold() + "%]."
            );
        }
    }

    @Transactional(readOnly = true)
    public List<ScalingEventDTO> getScalingEvents(UUID orgId, UUID workloadId) {
        return eventRepository.findByWorkloadIdOrderByExecutedAtDesc(workloadId).stream()
                .filter(e -> e.getScalingPolicy().getOrganisation().getId().equals(orgId))
                .map(ScalingEventDTO::fromEntity)
                .toList();
    }

    @Scheduled(fixedDelay = 30000) // Autonomous background scaling loop every 30s
    @Transactional
    public void evaluateAllActivePolicies() {
        List<ScalingPolicy> activePolicies = policyRepository.findAllActivePolicies();
        if (activePolicies.isEmpty()) {
            return;
        }

        log.debug("Autonomous autoscaling loop inspecting {} active policies", activePolicies.size());
        for (ScalingPolicy policy : activePolicies) {
            try {
                performEvaluation(policy, null);
            } catch (Exception e) {
                log.error("Error evaluating scaling policy {}", policy.getId(), e);
            }
        }
    }
}
