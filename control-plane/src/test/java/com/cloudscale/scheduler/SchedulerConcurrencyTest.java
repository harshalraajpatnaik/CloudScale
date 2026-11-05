package com.cloudscale.scheduler;

import com.cloudscale.common.exception.ConflictException;
import com.cloudscale.node.entity.Node;
import com.cloudscale.node.model.NodeStatus;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SchedulerConcurrencyTest {

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Test
    @DisplayName("Concurrent scheduling under pessimistic locking guarantees zero over-allocation")
    void testConcurrentSchedulingCapacityConstraint() throws InterruptedException {
        // 1. Create Organisation
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        Organisation org = organisationRepository.save(new Organisation(
                "Concurrency Test Org " + uniqueSuffix,
                "concurrency-test-" + uniqueSuffix
        ));
        UUID orgId = org.getId();

        // 2. Create single node: 8 Cores, 16384 MB (can strictly fit only 4 workloads of 2 Cores / 4096 MB)
        Node node = new Node(org, "worker-concurrency-" + uniqueSuffix, "192.168.10.101", 8, 16384L, true);
        node.setStatus(NodeStatus.ACTIVE);
        node = nodeRepository.save(node);

        // 3. Create 10 workloads in PENDING state
        List<UUID> workloadIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Workload workload = new Workload(
                    org,
                    "workload-" + uniqueSuffix + "-" + i,
                    WorkloadType.SERVICE,
                    WorkloadPriority.NORMAL,
                    2,
                    4096L
            );
            workload.setActualState(WorkloadState.PENDING);
            workload.setDesiredState(WorkloadState.RUNNING);
            workload = workloadRepository.save(workload);
            workloadIds.add(workload.getId());
        }

        // 4. Launch 10 concurrent threads to race for placement
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger capacityExhaustedCount = new AtomicInteger(0);
        AtomicInteger otherErrorsCount = new AtomicInteger(0);

        for (UUID wId : workloadIds) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // ensure simultaneous trigger
                    schedulerService.schedule(orgId, wId, "RESOURCE_AWARE");
                    successCount.incrementAndGet();
                } catch (ConflictException ce) {
                    if ("INSUFFICIENT_CAPACITY".equals(ce.getCode())) {
                        capacityExhaustedCount.incrementAndGet();
                    } else {
                        otherErrorsCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    otherErrorsCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire all threads at once!
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(otherErrorsCount.get()).isEqualTo(0);
        assertThat(successCount.get()).isEqualTo(4);
        assertThat(capacityExhaustedCount.get()).isEqualTo(6);

        // 5. Verify final Node capacity in DB: zero over-allocation
        Node updatedNode = nodeRepository.findById(node.getId()).orElseThrow();
        assertThat(updatedNode.getAllocatedCpuCores()).isEqualTo(8);
        assertThat(updatedNode.getAllocatedMemoryMb()).isEqualTo(16384L);
        assertThat(updatedNode.getAvailableCpuCores()).isEqualTo(0);
        assertThat(updatedNode.getAvailableMemoryMb()).isEqualTo(0L);

        // 6. Verify Workloads in DB: 4 RUNNING, 6 FAILED
        long runningCount = workloadRepository.findAll().stream()
                .filter(w -> orgId.equals(w.getOrganisation().getId()) && w.getActualState() == WorkloadState.RUNNING)
                .count();
        long failedCount = workloadRepository.findAll().stream()
                .filter(w -> orgId.equals(w.getOrganisation().getId()) && w.getActualState() == WorkloadState.FAILED)
                .count();

        assertThat(runningCount).isEqualTo(4);
        assertThat(failedCount).isEqualTo(6);
    }
}
