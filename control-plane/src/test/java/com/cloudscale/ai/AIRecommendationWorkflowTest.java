package com.cloudscale.ai;

import com.cloudscale.ai.client.AIServiceClient;
import com.cloudscale.ai.dto.AIRecommendationDTO;
import com.cloudscale.ai.dto.ChatResponseDTO;
import com.cloudscale.ai.dto.DiagnoseIncidentResponseDTO;
import com.cloudscale.ai.entity.AIRecommendation;
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

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIRecommendationWorkflowTest {

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

    private Organisation org;
    private UUID orgId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        userId = UUID.randomUUID();
        org = new Organisation("AI Test Org", "ai-test-org");
        org.setId(orgId);
    }

    @Test
    @DisplayName("Generate recommendations saves AI optimization in PENDING state")
    void testGenerateRecommendations() {
        when(organisationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(nodeRepository.findByOrganisationId(orgId)).thenReturn(List.of());
        when(workloadRepository.findAll()).thenReturn(List.of());

        Map<String, Object> mockRec = Map.of(
                "category", "NODE_REBALANCING",
                "title", "Rebalance worker-01",
                "explanation", "Host worker-01 operating near saturation threshold",
                "confidence_score", 0.92,
                "expected_impact", "Reduces host CPU to 55%",
                "suggested_action", "DRAIN_NODE",
                "action_payload", Map.of()
        );
        when(aiServiceClient.generateRecommendations(any())).thenReturn(List.of(mockRec));
        when(recommendationRepository.save(any(AIRecommendation.class))).thenAnswer(inv -> {
            AIRecommendation r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        List<AIRecommendationDTO> recs = aiService.generateRecommendations(orgId);

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0).category()).isEqualTo("NODE_REBALANCING");
        assertThat(recs.get(0).approvalStatus()).isEqualTo("PENDING");
        verify(recommendationRepository).save(any(AIRecommendation.class));
    }

    @Test
    @DisplayName("Operator approval transitions recommendation to APPROVED state")
    void testApproveRecommendation() {
        UUID recId = UUID.randomUUID();
        AIRecommendation rec = new AIRecommendation(org, "RIGHTSIZING", "Downscale idle service", "{}", new BigDecimal("0.900"), "Saves capacity", "SCALE_WORKLOAD", "{}");
        rec.setId(recId);

        when(recommendationRepository.findByIdAndOrganisationId(recId, orgId)).thenReturn(Optional.of(rec));
        when(recommendationRepository.save(any(AIRecommendation.class))).thenAnswer(inv -> inv.getArgument(0));

        AIRecommendationDTO approved = aiService.approveRecommendation(orgId, recId, userId);

        assertThat(approved.approvalStatus()).isEqualTo("APPROVED");
        verify(recommendationRepository).save(rec);
    }

    @Test
    @DisplayName("Operator rejection transitions recommendation to REJECTED state")
    void testRejectRecommendation() {
        UUID recId = UUID.randomUUID();
        AIRecommendation rec = new AIRecommendation(org, "RIGHTSIZING", "Downscale idle service", "{}", new BigDecimal("0.900"), "Saves capacity", "SCALE_WORKLOAD", "{}");
        rec.setId(recId);

        when(recommendationRepository.findByIdAndOrganisationId(recId, orgId)).thenReturn(Optional.of(rec));
        when(recommendationRepository.save(any(AIRecommendation.class))).thenAnswer(inv -> inv.getArgument(0));

        AIRecommendationDTO rejected = aiService.rejectRecommendation(orgId, recId);

        assertThat(rejected.approvalStatus()).isEqualTo("REJECTED");
        verify(recommendationRepository).save(rec);
    }

    @Test
    @DisplayName("AI Chat returns grounded response from client")
    void testChatWithAssistant() {
        when(nodeRepository.findByOrganisationId(orgId)).thenReturn(List.of());
        when(workloadRepository.findAll()).thenReturn(List.of());
        when(aiServiceClient.chat(anyString(), any(), any())).thenReturn("Host `worker-01` is operating at 78% CPU saturation.");

        ChatResponseDTO response = aiService.chat(orgId, "Which node has high CPU?", List.of());

        assertThat(response.reply()).contains("worker-01");
    }

    @Test
    @DisplayName("Diagnose incident returns structured root cause and remediation")
    void testDiagnoseIncident() {
        Map<String, Object> mockReport = Map.of(
                "title", "CPU Saturation Alert",
                "root_cause", "Host compute density exceeds safety limit",
                "severity", "HIGH",
                "affected_components", List.of("worker-01"),
                "remediation_steps", List.of("Drain host", "Scale replicas"),
                "confidence_score", 0.94
        );
        when(aiServiceClient.diagnoseIncident(any())).thenReturn(mockReport);

        DiagnoseIncidentResponseDTO report = aiService.diagnoseIncident(orgId, Map.of("cpu", 95.0));

        assertThat(report.title()).isEqualTo("CPU Saturation Alert");
        assertThat(report.severity()).isEqualTo("HIGH");
        assertThat(report.remediationSteps()).hasSize(2);
    }
}
