import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateScalingPolicyRequest, ScalingEvaluationResult, ScalingEvent, ScalingPolicy } from '../models/monitoring.models';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: { code: string; message: string; details?: any };
}

@Injectable({
  providedIn: 'root'
})
export class AutoscalingService {
  private http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/autoscaling';

  getPolicies(): Observable<ApiResponse<ScalingPolicy[]>> {
    return this.http.get<ApiResponse<ScalingPolicy[]>>(`${this.baseUrl}/policies`);
  }

  getPolicy(policyId: string): Observable<ApiResponse<ScalingPolicy>> {
    return this.http.get<ApiResponse<ScalingPolicy>>(`${this.baseUrl}/policies/${policyId}`);
  }

  createPolicy(req: CreateScalingPolicyRequest): Observable<ApiResponse<ScalingPolicy>> {
    return this.http.post<ApiResponse<ScalingPolicy>>(`${this.baseUrl}/policies`, req);
  }

  deletePolicy(policyId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/policies/${policyId}`);
  }

  evaluatePolicy(policyId: string, simulatedCpuUtil?: number): Observable<ApiResponse<ScalingEvaluationResult>> {
    let params = new HttpParams();
    if (simulatedCpuUtil !== undefined && simulatedCpuUtil !== null) {
      params = params.set('simulatedCpuUtil', simulatedCpuUtil.toString());
    }
    return this.http.post<ApiResponse<ScalingEvaluationResult>>(`${this.baseUrl}/policies/${policyId}/evaluate`, {}, { params });
  }

  getScalingEvents(workloadId: string): Observable<ApiResponse<ScalingEvent[]>> {
    return this.http.get<ApiResponse<ScalingEvent[]>>(`${this.baseUrl}/workloads/${workloadId}/events`);
  }
}
