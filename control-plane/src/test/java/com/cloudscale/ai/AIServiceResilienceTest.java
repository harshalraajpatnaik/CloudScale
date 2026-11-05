package com.cloudscale.ai;

import com.cloudscale.ai.client.AIServiceClient;
import com.cloudscale.ai.dto.ChatResponseDTO;
import com.cloudscale.ai.dto.DiagnoseIncidentResponseDTO;
import com.cloudscale.ai.repository.AIRecommendationRepository;
import com.cloudscale.ai.service.AIService;
import com.cloudscale.metrics.service.MetricsService;
import com.cloudscale.node.repository.NodeRepository;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import com.cloudscale.workload.repository.WorkloadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIServiceResilienceTest {

    @Mock
    private AIServiceClient aiServiceClient;

    @Mock
    private AIRecommendationRepository recommendationRepository;

    @Mock
    private OrganisationRepository organisationRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private WorkloadRepository workloadRepository;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private AIService aiService;

    private UUID orgId;
    private Organisation org;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        org = new Organisation("Resilience Org", "resilience-org");
        org.setId(orgId);
    }

    @Test
    @DisplayName("AI diagnose incident falls back safely when AI client returns fallback report")
    void testDiagnoseFallback() {
        Map<String, Object> fallback = Map.of(
                "title", "Cluster Host Capacity Alert",
                "root_cause", "Host operating above normal saturation threshold.",
                "severity", "HIGH",
                "affected_components", List.of("cluster-host"),
                "remediation_steps", List.of("Inspect active workloads"),
                "confidence_score", 0.88
        );
        when(aiServiceClient.diagnoseIncident(any())).thenReturn(fallback);

        DiagnoseIncidentResponseDTO res = aiService.diagnoseIncident(orgId, Map.of("severity", "HIGH"));

        assertThat(res.title()).isEqualTo("Cluster Host Capacity Alert");
        assertThat(res.severity()).isEqualTo("HIGH");
        assertThat(res.confidenceScore()).isEqualTo(0.88);
    }

    @Test
    @DisplayName("AI Chat returns structured response even under fallback condition")
    void testChatFallback() {
        when(nodeRepository.findByOrganisationId(orgId)).thenReturn(List.of());
        when(workloadRepository.findAll()).thenReturn(List.of());
        when(aiServiceClient.chat(any(), any(), any()))
                .thenReturn("CloudScale Control Plane AI Assistant: All cluster services are running nominal.");

        ChatResponseDTO reply = aiService.chat(orgId, "What is cluster status?", List.of());

        assertThat(reply.reply()).contains("CloudScale Control Plane AI Assistant");
    }
}
