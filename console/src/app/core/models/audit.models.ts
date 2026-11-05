export interface AuditLog {
  id: string;
  actorEmail: string;
  action: string;
  resourceType: string;
  resourceId: string;
  clientIp?: string;
  recordedAt: string;
}
