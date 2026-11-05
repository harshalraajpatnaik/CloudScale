import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  AIRecommendation,
  ChatRequest,
  ChatResponse,
  DiagnoseIncidentResponse,
  ForecastResponse
} from '../models/ai.models';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: string;
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class AiService {
  private readonly baseUrl = '/api/v1/ai';

  constructor(private http: HttpClient) {}

  listRecommendations(): Observable<AIRecommendation[]> {
    return this.http.get<ApiResponse<AIRecommendation[]>>(`${this.baseUrl}/recommendations`)
      .pipe(map(res => res.data));
  }

  generateRecommendations(): Observable<AIRecommendation[]> {
    return this.http.post<ApiResponse<AIRecommendation[]>>(`${this.baseUrl}/recommendations/generate`, {})
      .pipe(map(res => res.data));
  }

  approveRecommendation(id: string): Observable<AIRecommendation> {
    return this.http.post<ApiResponse<AIRecommendation>>(`${this.baseUrl}/recommendations/${id}/approve`, {})
      .pipe(map(res => res.data));
  }

  rejectRecommendation(id: string): Observable<AIRecommendation> {
    return this.http.post<ApiResponse<AIRecommendation>>(`${this.baseUrl}/recommendations/${id}/reject`, {})
      .pipe(map(res => res.data));
  }

  chat(request: ChatRequest): Observable<ChatResponse> {
    return this.http.post<ApiResponse<ChatResponse>>(`${this.baseUrl}/chat`, request)
      .pipe(map(res => res.data));
  }

  diagnoseIncident(incidentData: Record<string, any>): Observable<DiagnoseIncidentResponse> {
    return this.http.post<ApiResponse<DiagnoseIncidentResponse>>(`${this.baseUrl}/diagnose`, { incidentData })
      .pipe(map(res => res.data));
  }

  getNodeCapacityForecast(nodeId: string, durationMinutes: number = 60): Observable<ForecastResponse> {
    return this.http.get<ApiResponse<ForecastResponse>>(`${this.baseUrl}/forecast/nodes/${nodeId}?durationMinutes=${durationMinutes}`)
      .pipe(map(res => res.data));
  }
}
