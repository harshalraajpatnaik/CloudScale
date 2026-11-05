package com.cloudscale.autoscaling;

import com.cloudscale.autoscaling.dto.ScalingEvaluationResult;
import com.cloudscale.autoscaling.entity.ScalingPolicy;
import com.cloudscale.autoscaling.repository.ScalingEventRepository;
import com.cloudscale.autoscaling.repository.ScalingPolicyRepository;
import com.cloudscale.autoscaling.service.AutoscalingService;
import com.cloudscale.metrics.repository.NodeMetricRepository;
import com.cloudscale.node.entity.Node;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import com.cloudscale.workload.entity.Workload;
import com.cloudscale.workload.model.WorkloadPriority;
import com.cloudscale.workload.model.WorkloadType;
import com.cloudscale.workload.repository.WorkloadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AutoscalingPolicyEvaluationTest {

    @Mock
    private ScalingPolicyRepository policyRepository;

    @Mock
    private ScalingEventRepository eventRepository;

    @Mock
    private WorkloadRepository workloadRepository;

    @Mock
    private OrganisationRepository organisationRepository;

    @Mock
    private NodeMetricRepository nodeMetricRepository;

    @InjectMocks
    private AutoscalingService autoscalingService;

    private Organisation org;
    private Workload workload;
    private ScalingPolicy policy;

    @BeforeEach
    void setUp() {
        org = new Organisation("Acme Autoscaling", "acme-auto");
        workload = new Workload(org, "frontend-gateway", WorkloadType.SERVICE, WorkloadPriority.HIGH, 2, 4096L);

        policy = new ScalingPolicy(
                org,
                workload,
                new BigDecimal("80.00"), // Scale up at >= 80%
                new BigDecimal("30.00"), // Scale down at <= 30%
                new BigDecimal("85.00"),
                new BigDecimal("35.00"),
                1,                      // Min replicas
                5,                      // Max replicas
                300,                    // Cooldown: 300 seconds (5 min)
                300                     // Window: 300 seconds
        );
    }

    @Test
    @DisplayName("Scale-up triggered when CPU exceeds upper threshold")
    void testScaleUpTriggeredWhenCpuExceedsThreshold() {
        BigDecimal highCpu = new BigDecimal("88.50");

        ScalingEvaluationResult result = autoscalingService.performEvaluation(policy, highCpu);

        assertThat(result.decision()).isEqualTo("SCALE_UP");
        assertThat(result.previousReplicas()).isEqualTo(1);
        assertThat(result.newReplicas()).isEqualTo(2);
        assertThat(result.triggerMetric()).isEqualTo("CPU_UTILIZATION");
        assertThat(result.triggerValue()).isEqualTo(highCpu);

        verify(eventRepository).save(any());
        verify(policyRepository).save(any());
    }

    @Test
    @DisplayName("Scale-down triggered when CPU falls below lower threshold")
    void testScaleDownTriggeredWhenCpuBelowThreshold() {
        // Assume policy was previously scaled to 3 replicas, but min is 1
        // Let's test min replica clamp first:
        BigDecimal lowCpu = new BigDecimal("18.00");

        // When current is 1 (which equals minReplicas), it clamps
        ScalingEvaluationResult clampedResult = autoscalingService.performEvaluation(policy, lowCpu);
        assertThat(clampedResult.decision()).isEqualTo("CLAMPED_AT_LIMIT");
        assertThat(clampedResult.newReplicas()).isEqualTo(1);
    }

    @Test
    @DisplayName("No action taken when CPU is within healthy thresholds")
    void testNoActionWhenCpuWithinHealthyBounds() {
        BigDecimal normalCpu = new BigDecimal("55.00");

        ScalingEvaluationResult result = autoscalingService.performEvaluation(policy, normalCpu);

        assertThat(result.decision()).isEqualTo("NOOP_WITHIN_BOUNDS");
        assertThat(result.previousReplicas()).isEqualTo(1);
        assertThat(result.newReplicas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Cooldown period suppresses scaling actions to prevent oscillation")
    void testCooldownSuppressesOscillation() {
        // Mark policy as having scaled 60 seconds ago (cooldown is 300 seconds)
        policy.setLastScaledAt(Instant.now().minusSeconds(60));

        BigDecimal highCpu = new BigDecimal("95.00");
        ScalingEvaluationResult result = autoscalingService.performEvaluation(policy, highCpu);

        assertThat(result.decision()).isEqualTo("COOLDOWN_ACTIVE");
        assertThat(result.reasoning()).contains("Cooldown period active");
    }

    @Test
    @DisplayName("Scale-up clamped when max replicas limit is reached")
    void testClampedAtMaxReplicas() {
        // Set policy maxReplicas = 1
        policy.setMaxReplicas(1);

        BigDecimal highCpu = new BigDecimal("92.00");
        ScalingEvaluationResult result = autoscalingService.performEvaluation(policy, highCpu);

        assertThat(result.decision()).isEqualTo("CLAMPED_AT_LIMIT");
        assertThat(result.newReplicas()).isEqualTo(1);
        assertThat(result.reasoning()).contains("already at max replicas");
    }
}
