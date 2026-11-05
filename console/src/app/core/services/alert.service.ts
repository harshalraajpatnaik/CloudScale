import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Alert, AlertMetrics } from '../models/alert.models';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: string;
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  private readonly baseUrl = '/api/v1/alerts';

  constructor(private http: HttpClient) {}

  listAlerts(status: string = 'ALL'): Observable<Alert[]> {
    return this.http.get<ApiResponse<Alert[]>>(`${this.baseUrl}?status=${status}`)
      .pipe(map(res => res.data));
  }

  getMetrics(): Observable<AlertMetrics> {
    return this.http.get<ApiResponse<AlertMetrics>>(`${this.baseUrl}/metrics`)
      .pipe(map(res => res.data));
  }

  resolveAlert(id: string): Observable<Alert> {
    return this.http.post<ApiResponse<Alert>>(`${this.baseUrl}/${id}/resolve`, {})
      .pipe(map(res => res.data));
  }

  simulateAlert(payload?: { severity?: string; alertType?: string; message?: string }): Observable<Alert> {
    return this.http.post<ApiResponse<Alert>>(`${this.baseUrl}/simulate`, payload || {})
      .pipe(map(res => res.data));
  }
}
