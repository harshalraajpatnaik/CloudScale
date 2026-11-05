import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Node, NodeStatus, RegisterNodeRequest, UpdateNodeStatusRequest } from '../models/infrastructure.models';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: { code: string; message: string; details?: any };
}

@Injectable({
  providedIn: 'root'
})
export class NodeService {
  private http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/nodes';

  getNodes(): Observable<ApiResponse<Node[]>> {
    return this.http.get<ApiResponse<Node[]>>(this.baseUrl);
  }

  getNode(nodeId: string): Observable<ApiResponse<Node>> {
    return this.http.get<ApiResponse<Node>>(`${this.baseUrl}/${nodeId}`);
  }

  registerNode(request: RegisterNodeRequest): Observable<ApiResponse<Node>> {
    return this.http.post<ApiResponse<Node>>(this.baseUrl, request);
  }

  updateNodeStatus(nodeId: string, status: NodeStatus): Observable<ApiResponse<Node>> {
    return this.http.patch<ApiResponse<Node>>(`${this.baseUrl}/${nodeId}/status`, { status });
  }

  drainNode(nodeId: string): Observable<ApiResponse<Node>> {
    return this.updateNodeStatus(nodeId, 'DRAINING');
  }

  deleteNode(nodeId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/${nodeId}`);
  }
}
