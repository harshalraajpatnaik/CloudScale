# Testing Strategy & Quality Assurance — CloudScale

## 1. Testing Pyramid

```
                / \
               / E2E \              Playwright (Console UI, Auth, Workflows)
              /-------\
             /   INT   \            Spring Boot Integration Tests + Testcontainers
            /-----------\
           /    UNIT     \          JUnit 5, Mockito, pytest, Angular Jasmine/Vitest
          /---------------\
```

## 2. Test Suites by Layer

### A. Java Control Plane
- **Unit Tests (JUnit 5 + Mockito)**:
  - Scheduling algorithms (Best-Fit, Resource-Aware, First-Fit, Priority) tested with edge cases (empty cluster, exact fit, fragmentation).
  - Workload state machine transition logic.
  - Cost analytics calculations and underutilization formulas.
- **Integration Tests (Spring Boot + Testcontainers)**:
  - Flyway migration execution against real PostgreSQL in Docker.
  - Concurrency tests: 10 parallel threads attempting to reserve 10 workloads on a node with capacity for only 5. Proves 0 over-allocation.
  - Tenant isolation tests: User from Org A attempting to access Workload in Org B fails with 403 Forbidden.
  - RS256 token verification, rotation, and Redis revocation.

### B. Python AI Service
- **Unit & Pipeline Tests (pytest)**:
  - Data preprocessor: missing value imputation, chronological ordering.
  - Baseline comparison: LightGBM vs. 5-point rolling mean.
  - Anomaly detection scoring: Z-score and Isolation Forest detection rate.
  - Tool calling unit tests: Mocked Gemini responses verifying correct JSON payload serialization.

### C. Angular Console
- **Component Tests**:
  - Reactive form validation (email format, password complexity).
  - RBAC UI guards: Buttons disabled or hidden for `VIEWER` role.
  - Error and loading state visual rendering.
- **End-to-End Tests (Playwright)**:
  - Full user registration and organization onboarding.
  - Node registration, workload creation, and manual scheduling flow.
  - Autoscaling policy creation and alert notification rendering.

## 3. Concurrency Test Specification
```java
@Test
void whenTenConcurrentWorkloadsSubmitted_thenExactCapacityAllocatedWithoutOverProvisioning() throws Exception {
    int concurrentThreads = 10;
    Node node = createNodeWithCapacity(cpu = 8, memoryMb = 16384);
    // Workload requires 2 CPU, 4096 MB. Node fits exactly 4 workloads.
    
    CountDownLatch latch = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
    List<Future<ScheduleResult>> results = new ArrayList<>();
    
    for (int i = 0; i < concurrentThreads; i++) {
        results.add(executor.submit(() -> {
            latch.await();
            return schedulerService.schedule(createPendingWorkload(2, 4096));
        }));
    }
    
    latch.countDown(); // Release all threads concurrently
    
    int successful = 0, failed = 0;
    for (Future<ScheduleResult> f : results) {
        if (f.get().isSuccess()) successful++;
        else failed++;
    }
    
    assertEquals(4, successful, "Exactly 4 workloads must succeed");
    assertEquals(6, failed, "Remaining 6 workloads must fail due to capacity limits");
    
    Node updatedNode = nodeRepository.findById(node.getId()).orElseThrow();
    assertEquals(8, updatedNode.getAllocatedCpuCores());
    assertEquals(16384, updatedNode.getAllocatedMemoryMb());
}
```
