export type Role = 'SUPER_ADMIN' | 'ORGANISATION_ADMIN' | 'INFRASTRUCTURE_OPERATOR' | 'VIEWER';

export interface User {
  id: string;
  organisationId: string;
  email: string;
  fullName: string;
  role: Role;
  active: boolean;
  lastLoginAt?: string;
}

export interface Organisation {
  id: string;
  name: string;
  slug: string;
  planTier: string;
  maxCpuQuota: number;
  maxMemoryMbQuota: number;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
  user: User;
  organisation: Organisation;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  organisationName: string;
  fullName: string;
  email: string;
  password: string;
}
