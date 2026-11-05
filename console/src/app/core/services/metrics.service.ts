import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ClusterMetricsSummary, NodeMetric } from '../models/monitoring.models';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: { code: string; message: string; details?: any };
}

@Injectable({
  providedIn: 'root'
})
export class MetricsService {
  private http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/metrics';

  getNodeMetrics(nodeId: string, durationMinutes: number = 60): Observable<ApiResponse<NodeMetric[]>> {
    const params = new HttpParams().set('durationMinutes', durationMinutes.toString());
    return this.http.get<ApiResponse<NodeMetric[]>>(`${this.baseUrl}/nodes/${nodeId}`, { params });
  }

  getClusterSummary(durationMinutes: number = 60): Observable<ApiResponse<ClusterMetricsSummary>> {
    const params = new HttpParams().set('durationMinutes', durationMinutes.toString());
    return this.http.get<ApiResponse<ClusterMetricsSummary>>(`${this.baseUrl}/cluster/summary`, { params });
  }
}
