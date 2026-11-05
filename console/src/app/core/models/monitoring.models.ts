export interface NodeMetric {
  id: string;
  nodeId: string;
  hostname: string;
  cpuUtilizationPercent: number;
  memoryUtilizationPercent: number;
  diskReadBytesSec: number;
  diskWriteBytesSec: number;
  networkRxBytesSec: number;
  networkTxBytesSec: number;
  activeWorkloadCount: number;
  sampledAt: string;
}

export interface ClusterMetricsSummary {
  totalNodes: number;
  activeNodes: number;
  averageCpuUtilizationPercent: number;
  peakCpuUtilizationPercent: number;
  averageMemoryUtilizationPercent: number;
  peakMemoryUtilizationPercent: number;
  totalActiveWorkloads: number;
  windowStartTime: string;
  windowEndTime: string;
}

export interface ScalingPolicy {
  id: string;
  organisationId: string;
  workloadId: string;
  workloadName: string;
  cpuScaleUpThreshold: number;
  cpuScaleDownThreshold: number;
  memoryScaleUpThreshold: number;
  memoryScaleDownThreshold: number;
  minReplicas: number;
  maxReplicas: number;
  cooldownSeconds: number;
  evaluationWindowSeconds: number;
  isEnabled: boolean;
  lastScaledAt?: string;
  createdAt: string;
}

export interface CreateScalingPolicyRequest {
  workloadId: string;
  cpuScaleUpThreshold: number;
  cpuScaleDownThreshold: number;
  memoryScaleUpThreshold?: number;
  memoryScaleDownThreshold?: number;
  minReplicas: number;
  maxReplicas: number;
  cooldownSeconds: number;
  evaluationWindowSeconds: number;
}

export interface ScalingEvent {
  id: string;
  scalingPolicyId: string;
  workloadId: string;
  workloadName: string;
  actionType: 'SCALE_UP' | 'SCALE_DOWN' | 'NOOP';
  previousReplicas: number;
  newReplicas: number;
  triggerMetric: string;
  triggerValue: number;
  reasoning: string;
  executedAt: string;
}

export interface ScalingEvaluationResult {
  policyId: string;
  workloadId: string;
  decision: 'SCALE_UP' | 'SCALE_DOWN' | 'COOLDOWN_ACTIVE' | 'NOOP_WITHIN_BOUNDS' | 'CLAMPED_AT_LIMIT';
  previousReplicas: number;
  newReplicas: number;
  triggerMetric: string;
  triggerValue: number;
  reasoning: string;
}
