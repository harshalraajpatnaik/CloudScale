import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { CostSummary, NodeCost, WasteAnalysis, WorkloadCost } from '../models/cost.models';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: string;
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class CostService {
  private readonly baseUrl = '/api/v1/cost';

  constructor(private http: HttpClient) {}

  getSummary(): Observable<CostSummary> {
    return this.http.get<ApiResponse<CostSummary>>(`${this.baseUrl}/summary`)
      .pipe(map(res => res.data));
  }

  getNodeCosts(): Observable<NodeCost[]> {
    return this.http.get<ApiResponse<NodeCost[]>>(`${this.baseUrl}/nodes`)
      .pipe(map(res => res.data));
  }

  getWorkloadCosts(): Observable<WorkloadCost[]> {
    return this.http.get<ApiResponse<WorkloadCost[]>>(`${this.baseUrl}/workloads`)
      .pipe(map(res => res.data));
  }

  getWasteAnalysis(): Observable<WasteAnalysis> {
    return this.http.get<ApiResponse<WasteAnalysis>>(`${this.baseUrl}/waste`)
      .pipe(map(res => res.data));
  }
}
