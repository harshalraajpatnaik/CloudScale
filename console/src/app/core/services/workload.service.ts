import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateWorkloadRequest, ScheduleWorkloadRequest, SchedulingAlgorithmType, Workload } from '../models/infrastructure.models';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: { code: string; message: string; details?: any };
}

@Injectable({
  providedIn: 'root'
})
export class WorkloadService {
  private http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/workloads';

  getWorkloads(): Observable<ApiResponse<Workload[]>> {
    return this.http.get<ApiResponse<Workload[]>>(this.baseUrl);
  }

  getWorkload(workloadId: string): Observable<ApiResponse<Workload>> {
    return this.http.get<ApiResponse<Workload>>(`${this.baseUrl}/${workloadId}`);
  }

  createWorkload(request: CreateWorkloadRequest): Observable<ApiResponse<Workload>> {
    return this.http.post<ApiResponse<Workload>>(this.baseUrl, request);
  }

  scheduleWorkload(workloadId: string, algorithm: SchedulingAlgorithmType = 'RESOURCE_AWARE'): Observable<ApiResponse<Workload>> {
    const payload: ScheduleWorkloadRequest = { algorithm };
    return this.http.post<ApiResponse<Workload>>(`${this.baseUrl}/${workloadId}/schedule`, payload);
  }

  stopWorkload(workloadId: string): Observable<ApiResponse<Workload>> {
    return this.http.post<ApiResponse<Workload>>(`${this.baseUrl}/${workloadId}/stop`, {});
  }

  restartWorkload(workloadId: string, algorithm: SchedulingAlgorithmType = 'RESOURCE_AWARE'): Observable<ApiResponse<Workload>> {
    const payload: ScheduleWorkloadRequest = { algorithm };
    return this.http.post<ApiResponse<Workload>>(`${this.baseUrl}/${workloadId}/restart`, payload);
  }

  suspendWorkload(workloadId: string): Observable<ApiResponse<Workload>> {
    return this.http.post<ApiResponse<Workload>>(`${this.baseUrl}/${workloadId}/suspend`, {});
  }

  deleteWorkload(workloadId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${workloadId}`);
  }
}
