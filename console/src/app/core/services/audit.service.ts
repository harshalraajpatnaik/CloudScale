import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { AuditLog } from '../models/audit.models';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: string;
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  private readonly baseUrl = '/api/v1/audit';

  constructor(private http: HttpClient) {}

  getRecentLogs(limit: number = 50): Observable<AuditLog[]> {
    return this.http.get<ApiResponse<AuditLog[]>>(`${this.baseUrl}/logs?limit=${limit}`)
      .pipe(map(res => res.data));
  }
}
