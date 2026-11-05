export interface Alert {
  id: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  alertType: string;
  message: string;
  status: 'ACTIVE' | 'RESOLVED' | 'ACKNOWLEDGED';
  nodeHostname?: string;
  workloadName?: string;
  triggeredAt: string;
  resolvedAt?: string;
  durationMinutes?: number;
}

export interface AlertMetrics {
  totalActive: number;
  criticalCount: number;
  highCount: number;
  resolvedPast24Hours: number;
  meanTimeToResolutionMinutes: number;
}
