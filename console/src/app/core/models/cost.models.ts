export interface CostSummary {
  hourlyBurnRateUsd: number;
  dailyBurnRateUsd: number;
  monthlyBurnRateUsd: number;
  totalCpuCoresAllocated: number;
  totalMemoryGbAllocated: number;
  activeWorkloadsCount: number;
  totalNodesCount: number;
  currency: string;
}

export interface NodeCost {
  nodeId: string;
  hostname: string;
  allocatedCores: number;
  totalCores: number;
  allocatedMemoryGb: number;
  totalMemoryGb: number;
  hourlyCostUsd: number;
  monthlyCostUsd: number;
  utilizationPercent: number;
  isUnderutilized: boolean;
}

export interface WorkloadCost {
  workloadId: string;
  name: string;
  assignedNode: string;
  cpuCores: number;
  memoryGb: number;
  hourlyCostUsd: number;
  monthlyCostUsd: number;
  priority: string;
  state: string;
}

export interface WasteAnalysis {
  monthlyWastedSpendUsd: number;
  idleCoresCount: number;
  idleMemoryGb: number;
  underutilizedNodesCount: number;
  potentialSavingsPercent: number;
  recommendations: string[];
}
