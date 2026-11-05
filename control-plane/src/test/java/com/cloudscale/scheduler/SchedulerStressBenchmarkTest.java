package com.cloudscale.scheduler;

import com.cloudscale.common.exception.ConflictException;
import com.cloudscale.node.entity.Node;
import com.cloudscale.node.repository.NodeRepository;
import com.cloudscale.scheduler.service.SchedulerService;
import com.cloudscale.tenancy.entity.Organisation;
import com.cloudscale.tenancy.repository.OrganisationRepository;
import com.cloudscale.workload.entity.Workload;
import com.cloudscale.workload.model.WorkloadPriority;
import com.cloudscale.workload.model.WorkloadState;
import com.cloudscale.workload.model.WorkloadType;
import com.cloudscale.workload.repository.WorkloadRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SchedulerStressBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(SchedulerStressBenchmarkTest.class);

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    private SchedulerService schedulerService;

    @Test
    @DisplayName("20 concurrent workload submissions across 4 nodes adhere to strict capacity limits with zero over-allocation")
    void testConcurrentSchedulerStressBenchmark() throws InterruptedException {
        String runSuffix = UUID.randomUUID().toString().substring(0, 8);
        Organisation org = organisationRepository.save(new Organisation("Stress Org " + runSuffix, "stress-" + runSuffix));

        // Create 4 worker nodes with 8 cores each (Total capacity: 32 cores)
        List<Node> nodes = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Node node = new Node(org, "stress-worker-" + runSuffix + "-" + i, "10.100.1." + i, 8, 16384L, true);
            nodes.add(nodeRepository.save(node));
        }

        // Create 20 workloads requesting 2 cores each (Total demand: 40 cores; Cluster capacity: 32 cores)
        // Expected outcome: exactly 16 succeed (32 cores allocated), exactly 4 fail (capacity exhaustion).
        List<Workload> workloads = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Workload w = new Workload(
                    org,
                    "stress-job-" + runSuffix + "-" + i,
                    WorkloadType.SERVICE,
                    WorkloadPriority.HIGH,
                    2,
                    2048L
            );
            workloads.add(workloadRepository.save(w));
        }

        int concurrencyLevel = 20;
        ExecutorService executor = Executors.newFixedThreadPool(concurrencyLevel);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrencyLevel);

        AtomicInteger scheduledCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        long startTime = System.nanoTime();

        for (Workload w : workloads) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize all 20 threads to hit scheduler at exact same instant
                    schedulerService.schedule(org.getId(), w.getId(), "RESOURCE_AWARE");
                    scheduledCount.incrementAndGet();
                } catch (ConflictException ce) {
                    rejectedCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("Unexpected error during stress benchmark", e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Fire all 20 threads concurrently
        boolean finished = doneLatch.await(30, TimeUnit.SECONDS);
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        executor.shutdown();

        assertThat(finished).isTrue();
        log.info("Benchmark complete in {} ms: {} scheduled, {} rejected out of 20 concurrent requests",
                durationMs, scheduledCount.get(), rejectedCount.get());

        // Assert exact capacity limits
        assertThat(scheduledCount.get()).isEqualTo(16);
        assertThat(rejectedCount.get()).isEqualTo(4);

        // Verify total allocated cores across all nodes is exactly 32
        int totalAllocated = 0;
        for (Node n : nodes) {
            Node refreshed = nodeRepository.findById(n.getId()).orElseThrow();
            assertThat(refreshed.getAllocatedCpuCores()).isLessThanOrEqualTo(8);
            totalAllocated += refreshed.getAllocatedCpuCores();
        }
        assertThat(totalAllocated).isEqualTo(32);

        // Verify database state: 16 RUNNING, 4 FAILED
        long runningWorkloads = workloadRepository.findAll().stream()
                .filter(w -> w.getOrganisation().getId().equals(org.getId()) && w.getActualState() == WorkloadState.RUNNING)
                .count();
        long failedWorkloads = workloadRepository.findAll().stream()
                .filter(w -> w.getOrganisation().getId().equals(org.getId()) && w.getActualState() == WorkloadState.FAILED)
                .count();

        assertThat(runningWorkloads).isEqualTo(16);
        assertThat(failedWorkloads).isEqualTo(4);
    }
}
