# ADR-006: Autoscaling Strategy — Deterministic Rule-Based with Predictive Foresight

## Status
Accepted

## Context
Autoscaling workloads based solely on reactive thresholds (e.g., current CPU > 80%) often causes scaling lag: by the time new replicas boot up, the sudden traffic spike has already impacted end users. However, relying purely on black-box ML predictions risks erratic scaling behavior or runaway costs.

## Decision
We implement a two-tiered autoscaling architecture:
1. **Tier 1: Deterministic Rule-Based Autoscaler (Foundation)**:
   - Evaluates real metrics over a sliding window (default 5 minutes).
   - Enforces min/max replica boundaries and configurable cooldown periods (default 300s).
   - Acts as the reliable, deterministic fallback.
2. **Tier 2: Predictive Foresight Controller (Augmentation)**:
   - Queries the Python AI service for 15-minute and 30-minute capacity forecasts.
   - If a sustained trend breach is predicted with high confidence (>85%) and current utilization is already elevated, it pre-scales replicas incrementally prior to the peak.
   - For dramatic scaling shifts (>50% replica surge), the system generates an AI Recommendation requiring human operator approval before execution.

## Consequences
- **Positive**: Mitigates reactive scaling lag while safeguarding against erratic, unconstrained machine learning actions; guarantees human oversight for significant infrastructure spend.
- **Negative**: Increases state coordination complexity within the autoscaling controller.
