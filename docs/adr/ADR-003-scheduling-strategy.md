# ADR-003: Workload Scheduling Strategy & Default Algorithm

## Status
Accepted

## Context
When a workload is scheduled, the engine must select the optimal node while respecting CPU and memory capacities, preventing fragmentation, and handling concurrent allocation requests without race conditions.

## Decision
We implement a pluggable `SchedulingAlgorithm` interface with four implementations:
1. `ResourceAwareScheduler` (**Default**): Calculates a multi-dimensional balance score evaluating remaining CPU and memory ratios to prevent stranded resources.
2. `BestFitScheduler`: Packs workloads onto the most utilized node that can still fit the workload, maximizing consolidation.
3. `FirstFitScheduler`: Selects the first eligible node in $O(n)$ time.
4. `PriorityScheduler`: Enforces priority preemption for `CRITICAL` workloads over `BATCH` workloads.

Concurrency control is guaranteed using database row-level locking (`SELECT ... FOR UPDATE SKIP LOCKED`) during host evaluation and transactional reservation.

## Consequences
- **Positive**: Prevents resource starvation and fragmentation; default Resource-Aware scheduler optimizes dual CPU/memory efficiency; pessimistic locking prevents race conditions.
- **Negative**: Best-Fit and Resource-Aware algorithms exhibit $O(n \log n)$ time complexity compared to $O(n)$ for First-Fit.
