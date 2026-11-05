package com.cloudscale.metrics.service;

import com.cloudscale.alert.entity.Alert;
import com.cloudscale.alert.repository.AlertRepository;
import com.cloudscale.alert.service.AlertService;
import com.cloudscale.metrics.entity.NodeMetric;
import com.cloudscale.metrics.repository.NodeMetricRepository;
import com.cloudscale.node.entity.Node;
import com.cloudscale.node.model.NodeStatus;
import com.cloudscale.node.repository.NodeRepository;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import com.cloudscale.workload.entity.Workload;
import com.cloudscale.workload.repository.WorkloadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

@Service
public class SimulatedTelemetryService {

    private static final Logger log = LoggerFactory.getLogger(SimulatedTelemetryService.class);
    private final Random random = new Random();

    private final NodeRepository nodeRepository;
    private final NodeMetricRepository nodeMetricRepository;
    private final WorkloadRepository workloadRepository;
    private final AlertRepository alertRepository;
    private final AlertService alertService;
    private final OrganisationRepository organisationRepository;

    public SimulatedTelemetryService(
            NodeRepository nodeRepository,
            NodeMetricRepository nodeMetricRepository,
            WorkloadRepository workloadRepository,
            AlertRepository alertRepository,
            AlertService alertService,
            OrganisationRepository organisationRepository
    ) {
        this.nodeRepository = nodeRepository;
        this.nodeMetricRepository = nodeMetricRepository;
        this.workloadRepository = workloadRepository;
        this.alertRepository = alertRepository;
        this.alertService = alertService;
        this.organisationRepository = organisationRepository;
    }

    /**
     * Seeds historical telemetry and default incident records on startup if absent.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedInitialTelemetryAndIncidents() {
        log.info("Checking simulated nodes telemetry and incident presence...");
        List<Node> simulatedNodes = nodeRepository.findSimulatedNodes();
        Instant now = Instant.now();

        for (Node node : simulatedNodes) {
            // Check if node has metrics in the past 2 hours
            Instant past2h = now.minus(2, ChronoUnit.HOURS);
            List<NodeMetric> recent = nodeMetricRepository.findByNodeIdSince(node.getId(), past2h);

            if (recent.size() < 15) {
                log.info("Backfilling 20 historical metric points for node {}", node.getHostname());
                int allocatedCpu = node.getAllocatedCpuCores();
                double cpuAllocRatio = (double) allocatedCpu / Math.max(1, node.getTotalCpuCores());
                double memAllocRatio = (double) node.getAllocatedMemoryMb() / Math.max(1L, node.getTotalMemoryMb());

                // Backfill 20 points over last 60 minutes (1 sample every 3 minutes)
                for (int i = 20; i >= 1; i--) {
                    Instant sampleTime = now.minus((long) i * 3, ChronoUnit.MINUTES);
                    double harmonicWave = Math.sin((20 - i) * 0.4) * 6.5;
                    double jitter = (random.nextDouble() * 4.0) - 2.0;

                    double baseCpu = 25.0 + (cpuAllocRatio * 55.0) + harmonicWave + jitter;
                    double baseMem = 30.0 + (memAllocRatio * 50.0) + ((random.nextDouble() * 2.0) - 1.0);

                    BigDecimal cpu = BigDecimal.valueOf(Math.max(8.0, Math.min(96.0, baseCpu))).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal mem = BigDecimal.valueOf(Math.max(15.0, Math.min(95.0, baseMem))).setScale(2, RoundingMode.HALF_UP);

                    NodeMetric sample = new NodeMetric(
                            node,
                            cpu,
                            mem,
                            100000L + random.nextInt(300000),
                            150000L + random.nextInt(400000),
                            200000L + random.nextInt(500000),
                            120000L + random.nextInt(300000),
                            Math.max(1, allocatedCpu / 2),
                            sampleTime
                    );
                    nodeMetricRepository.save(sample);
                }
            }
        }

        // Seed realistic incident alerts if an organization has none
        List<Organisation> organisations = organisationRepository.findAll();
        for (Organisation org : organisations) {
            long alertCount = alertRepository.countByOrganisationIdAndStatus(org.getId(), "ACTIVE")
                    + alertRepository.countByOrganisationIdAndStatus(org.getId(), "RESOLVED");

            if (alertCount == 0) {
                log.info("Seeding realistic starter incidents for organization {}", org.getName());
                List<Node> orgNodes = nodeRepository.findByOrganisationId(org.getId());
                Node targetNode = !orgNodes.isEmpty() ? orgNodes.get(0) : null;

                // 1. One Active Warning Alert
                alertService.createAlert(
                        org.getId(),
                        targetNode != null ? targetNode.getId() : null,
                        null,
                        "HIGH",
                        "HOST_SATURATION_WARNING",
                        "Host CPU core saturation sustained above 85% for >3 consecutive metric evaluation cycles."
                );

                // 2. One Past Resolved Alert (Resolved 35 minutes ago, lasting 18 min)
                Alert resolvedAlert = new Alert(
                        org,
                        targetNode,
                        null,
                        "CRITICAL",
                        "MEMORY_PRESSURE_SPIKE",
                        "Memory saturation exceeded 92% SLA limit on compute worker host."
                );
                resolvedAlert.setTriggeredAt(now.minus(53, ChronoUnit.MINUTES));
                resolvedAlert.setStatus("RESOLVED");
                resolvedAlert.setResolvedAt(now.minus(35, ChronoUnit.MINUTES));
                alertRepository.save(resolvedAlert);
            }
        }
    }

    /**
     * Continuous 15-second telemetry heartbeat and real-time metric sampling for simulated nodes.
     */
    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void sampleSimulatedNodesTelemetry() {
        List<Node> simulatedNodes = nodeRepository.findSimulatedNodes();
        if (simulatedNodes.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        for (Node node : simulatedNodes) {
            if (node.getStatus() != NodeStatus.ACTIVE) {
                continue;
            }

            int allocatedCpu = node.getAllocatedCpuCores();
            long allocatedMem = node.getAllocatedMemoryMb();

            double cpuAllocRatio = (double) allocatedCpu / Math.max(1, node.getTotalCpuCores());
            double memAllocRatio = (double) allocatedMem / Math.max(1L, node.getTotalMemoryMb());

            // Wave dynamic variation based on clock seconds
            double cycle = Math.sin((System.currentTimeMillis() / 30000.0)) * 5.5;
            double noise = (random.nextDouble() * 3.0) - 1.5;

            double cpu = 20.0 + (cpuAllocRatio * 60.0) + cycle + noise;
            double mem = 25.0 + (memAllocRatio * 55.0) + ((random.nextDouble() * 2.0) - 1.0);

            BigDecimal cpuVal = BigDecimal.valueOf(Math.max(5.0, Math.min(98.5, cpu))).setScale(2, RoundingMode.HALF_UP);
            BigDecimal memVal = BigDecimal.valueOf(Math.max(10.0, Math.min(98.0, mem))).setScale(2, RoundingMode.HALF_UP);

            List<Workload> workloads = workloadRepository.findByAssignedNodeId(node.getId());

            NodeMetric metric = new NodeMetric(
                    node,
                    cpuVal,
                    memVal,
                    80000L + random.nextInt(250000),
                    120000L + random.nextInt(350000),
                    150000L + random.nextInt(400000),
                    90000L + random.nextInt(200000),
                    workloads.size(),
                    now
            );
            nodeMetricRepository.save(metric);

            // Keep node heartbeat updated
            node.setLastHeartbeat(now);
            nodeRepository.save(node);
        }
    }
}
