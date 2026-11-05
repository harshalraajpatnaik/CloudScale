export type NodeStatus = 'ACTIVE' | 'DRAINING' | 'MAINTENANCE' | 'INACTIVE' | 'UNREACHABLE';

export interface Node {
  id: string;
  organisationId: string;
  hostname: string;
  ipAddress: string;
  totalCpuCores: number;
  totalMemoryMb: number;
  allocatedCpuCores: number;
  allocatedMemoryMb: number;
  availableCpuCores: number;
  availableMemoryMb: number;
  cpuUtilizationPercent: number;
  memoryUtilizationPercent: number;
  status: NodeStatus;
  isSimulated: boolean;
  lastHeartbeat: string;
  createdAt: string;
}

export interface RegisterNodeRequest {
  hostname: string;
  ipAddress: string;
  totalCpuCores: number;
  totalMemoryMb: number;
  simulated: boolean;
}

export interface UpdateNodeStatusRequest {
  status: NodeStatus;
}

export type WorkloadState = 'PENDING' | 'SCHEDULING' | 'RUNNING' | 'STOPPING' | 'STOPPED' | 'SUSPENDED' | 'FAILED' | 'COMPLETED';

export type WorkloadPriority = 'CRITICAL' | 'HIGH' | 'NORMAL' | 'LOW' | 'BATCH';

export type WorkloadType = 'SERVICE' | 'BATCH_JOB' | 'DATABASE' | 'INGRESS_CONTROLLER' | 'BACKGROUND_WORKER';

export type SchedulingAlgorithmType = 'RESOURCE_AWARE' | 'BEST_FIT' | 'FIRST_FIT' | 'PRIORITY';

export interface Workload {
  id: string;
  organisationId: string;
  assignedNodeId?: string;
  assignedNodeHostname?: string;
  name: string;
  workloadType: WorkloadType;
  priority: WorkloadPriority;
  cpuCoresRequired: number;
  memoryMbRequired: number;
  desiredState: WorkloadState;
  actualState: WorkloadState;
  failureReason?: string;
  scheduledAt?: string;
  createdAt: string;
}

export interface CreateWorkloadRequest {
  name: string;
  workloadType: WorkloadType;
  priority: WorkloadPriority;
  cpuCoresRequired: number;
  memoryMbRequired: number;
}

export interface ScheduleWorkloadRequest {
  algorithm?: SchedulingAlgorithmType;
}
