package com.cloudscale.ai.service;

import com.cloudscale.ai.client.AIServiceClient;
import com.cloudscale.ai.dto.AIRecommendationDTO;
import com.cloudscale.ai.dto.ChatResponseDTO;
import com.cloudscale.ai.dto.DiagnoseIncidentResponseDTO;
import com.cloudscale.ai.dto.ForecastResponseDTO;
import com.cloudscale.ai.entity.AIRecommendation;
import com.cloudscale.ai.repository.AIRecommendationRepository;
import com.cloudscale.common.exception.ResourceNotFoundException;
import com.cloudscale.metrics.dto.NodeMetricDTO;
import com.cloudscale.metrics.service.MetricsService;
import com.cloudscale.node.entity.Node;
import com.cloudscale.node.repository.NodeRepository;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import com.cloudscale.workload.entity.Workload;
import com.cloudscale.workload.repository.WorkloadRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    private final AIServiceClient aiServiceClient;
    private final AIRecommendationRepository recommendationRepository;
    private final OrganisationRepository organisationRepository;
    private final NodeRepository nodeRepository;
    private final WorkloadRepository workloadRepository;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;

    public AIService(
            AIServiceClient aiServiceClient,
            AIRecommendationRepository recommendationRepository,
            OrganisationRepository organisationRepository,
            NodeRepository nodeRepository,
            WorkloadRepository workloadRepository,
            MetricsService metricsService
    ) {
        this.aiServiceClient = aiServiceClient;
        this.recommendationRepository = recommendationRepository;
        this.organisationRepository = organisationRepository;
        this.nodeRepository = nodeRepository;
        this.workloadRepository = workloadRepository;
        this.metricsService = metricsService;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional(readOnly = true)
    public List<AIRecommendationDTO> listRecommendations(UUID orgId) {
        return recommendationRepository.findAllByOrganisationIdOrderByEvaluatedAtDesc(orgId).stream()
                .map(AIRecommendationDTO::fromEntity)
                .toList();
    }

    @Transactional
    public List<AIRecommendationDTO> generateRecommendations(UUID orgId) {
        Organisation org = organisationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ORG_NOT_FOUND", "Organisation not found"));

        Map<String, Object> clusterContext = buildClusterContext(orgId);
        List<Map<String, Object>> recDataList = aiServiceClient.generateRecommendations(clusterContext);

        List<AIRecommendationDTO> results = new ArrayList<>();

        for (Map<String, Object> recMap : recDataList) {
            String category = (String) recMap.getOrDefault("category", "RIGHTSIZING");
            String explanation = (String) recMap.getOrDefault("explanation", (String) recMap.getOrDefault("title", "Optimization Recommendation"));
            BigDecimal confidence = recMap.get("confidence_score") != null 
                    ? new BigDecimal(recMap.get("confidence_score").toString()) 
                    : new BigDecimal("0.850");
            String impact = (String) recMap.getOrDefault("expected_impact", "Performance optimization");
            String action = (String) recMap.getOrDefault("suggested_action", "NOOP");

            String payloadJson = "{}";
            try {
                if (recMap.get("action_payload") != null) {
                    payloadJson = objectMapper.writeValueAsString(recMap.get("action_payload"));
                }
            } catch (Exception ignored) {}

            AIRecommendation entity = new AIRecommendation(
                    org,
                    category,
                    explanation,
                    "{}",
                    confidence,
                    impact,
                    action,
                    payloadJson
            );

            AIRecommendation saved = recommendationRepository.save(entity);
            results.add(AIRecommendationDTO.fromEntity(saved));
        }

        log.info("Generated and saved {} AI infrastructure recommendations for organisation {}", results.size(), org.getName());
        return results;
    }

    @Transactional
    public AIRecommendationDTO approveRecommendation(UUID orgId, UUID recId, UUID userId) {
        AIRecommendation rec = recommendationRepository.findByIdAndOrganisationId(recId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("REC_NOT_FOUND", "Recommendation not found"));

        rec.setApprovalStatus("APPROVED");
        rec.setApprovedByUserId(userId);
        AIRecommendation saved = recommendationRepository.save(rec);
        log.info("Operator approved AI recommendation {}: {}", recId, rec.getCategory());
        return AIRecommendationDTO.fromEntity(saved);
    }

    @Transactional
    public AIRecommendationDTO rejectRecommendation(UUID orgId, UUID recId) {
        AIRecommendation rec = recommendationRepository.findByIdAndOrganisationId(recId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("REC_NOT_FOUND", "Recommendation not found"));

        rec.setApprovalStatus("REJECTED");
        AIRecommendation saved = recommendationRepository.save(rec);
        log.info("Operator rejected AI recommendation {}", recId);
        return AIRecommendationDTO.fromEntity(saved);
    }

    public ChatResponseDTO chat(UUID orgId, String userMessage, List<Map<String, String>> history) {
        Map<String, Object> context = buildClusterContext(orgId);
        String reply = aiServiceClient.chat(userMessage, history, context);
        return new ChatResponseDTO(reply);
    }

    public DiagnoseIncidentResponseDTO diagnoseIncident(UUID orgId, Map<String, Object> incidentData) {
        Map<String, Object> report = aiServiceClient.diagnoseIncident(incidentData);
        return new DiagnoseIncidentResponseDTO(
                (String) report.getOrDefault("title", "Infrastructure Saturation Incident"),
                (String) report.getOrDefault("root_cause", "Host operating above designated saturation limits."),
                (String) report.getOrDefault("severity", "HIGH"),
                (List<String>) report.getOrDefault("affected_components", List.of("compute-node")),
                (List<String>) report.getOrDefault("remediation_steps", List.of("Investigate workload consumption", "Drain node")),
                report.get("confidence_score") != null ? Double.parseDouble(report.get("confidence_score").toString()) : 0.85
        );
    }

    public ForecastResponseDTO forecastNodeCapacity(UUID orgId, UUID nodeId, int durationMinutes) {
        List<NodeMetricDTO> metrics = metricsService.getNodeMetrics(orgId, nodeId, durationMinutes);
        List<Map<String, Object>> history = metrics.stream()
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("timestamp", m.sampledAt().toString());
                    map.put("cpuUtilizationPercent", m.cpuUtilizationPercent().doubleValue());
                    map.put("memoryUtilizationPercent", m.memoryUtilizationPercent().doubleValue());
                    return map;
                })
                .toList();

        Map<String, Object> result = aiServiceClient.forecastCapacity(history, "cpuUtilizationPercent", 12);
        return new ForecastResponseDTO(
                (List<Map<String, Object>>) result.getOrDefault("evaluatedModels", List.of()),
                (String) result.getOrDefault("selectedModel", "Moving Average"),
                (Map<String, Object>) result.getOrDefault("modelPerformance", Map.of()),
                result.get("forecastHorizonMinutes") != null ? (int) result.get("forecastHorizonMinutes") : 60,
                (List<Map<String, Object>>) result.getOrDefault("forecast", List.of())
        );
    }

    private Map<String, Object> buildClusterContext(UUID orgId) {
        List<Node> nodes = nodeRepository.findByOrganisationId(orgId);
        List<Workload> workloads = workloadRepository.findAll().stream()
                .filter(w -> w.getOrganisation().getId().equals(orgId))
                .toList();

        List<Map<String, Object>> nodeData = nodes.stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId().toString());
            map.put("hostname", n.getHostname());
            map.put("status", n.getStatus().name());
            map.put("totalCpuCores", n.getTotalCpuCores());
            map.put("allocatedCpuCores", n.getAllocatedCpuCores());
            map.put("cpuUtilizationPercent", n.getTotalCpuCores() > 0 ? (double) n.getAllocatedCpuCores() / n.getTotalCpuCores() * 100 : 0);
            return map;
        }).toList();

        List<Map<String, Object>> workloadData = workloads.stream().map(w -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", w.getId().toString());
            map.put("name", w.getName());
            map.put("actualState", w.getActualState().name());
            map.put("cpuCores", w.getCpuCoresRequired());
            map.put("memoryMb", w.getMemoryMbRequired());
            map.put("assignedNode", w.getAssignedNode() != null ? w.getAssignedNode().getHostname() : null);
            return map;
        }).toList();

        return Map.of(
                "nodes", nodeData,
                "workloads", workloadData,
                "totalNodes", nodes.size(),
                "totalWorkloads", workloads.size()
        );
    }
}
