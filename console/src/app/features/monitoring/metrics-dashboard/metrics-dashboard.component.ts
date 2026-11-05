import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MetricsService } from '../../../core/services/metrics.service';
import { NodeService } from '../../../core/services/node.service';
import { ClusterMetricsSummary, NodeMetric } from '../../../core/models/monitoring.models';
import { Node } from '../../../core/models/infrastructure.models';

@Component({
  selector: 'app-metrics-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './metrics-dashboard.component.html',
  styleUrls: ['./metrics-dashboard.component.scss']
})
export class MetricsDashboardComponent implements OnInit, OnDestroy {
  private metricsService = inject(MetricsService);
  private nodeService = inject(NodeService);

  nodes = signal<Node[]>([]);
  selectedNodeId = signal<string | null>(null);
  metrics = signal<NodeMetric[]>([]);
  clusterSummary = signal<ClusterMetricsSummary | null>(null);

  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  // Time window and refresh
  selectedDuration = 60; // minutes
  refreshInterval = 15; // seconds
  private refreshTimer: any;

  ngOnInit(): void {
    this.fetchNodesAndInitialData();
    this.setupRefreshTimer();
  }

  ngOnDestroy(): void {
    this.clearRefreshTimer();
  }

  fetchNodesAndInitialData(): void {
    this.isLoading.set(true);
    this.nodeService.getNodes().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.nodes.set(res.data);
          if (res.data.length > 0 && !this.selectedNodeId()) {
            this.selectedNodeId.set(res.data[0].id);
          }
          this.refreshAllData();
        }
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to load compute nodes');
        this.isLoading.set(false);
      }
    });
  }

  refreshAllData(): void {
    this.fetchClusterSummary();
    if (this.selectedNodeId()) {
      this.fetchNodeMetrics(this.selectedNodeId()!);
    } else {
      this.isLoading.set(false);
    }
  }

  fetchClusterSummary(): void {
    this.metricsService.getClusterSummary(this.selectedDuration).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.clusterSummary.set(res.data);
        }
      }
    });
  }

  fetchNodeMetrics(nodeId: string): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.metricsService.getNodeMetrics(nodeId, this.selectedDuration).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.metrics.set(res.data);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to load telemetry samples');
        this.isLoading.set(false);
      }
    });
  }

  onNodeSelected(nodeId: string): void {
    this.selectedNodeId.set(nodeId);
    this.fetchNodeMetrics(nodeId);
  }

  onDurationChange(duration: number): void {
    this.selectedDuration = duration;
    this.refreshAllData();
  }

  onIntervalChange(interval: number): void {
    this.refreshInterval = interval;
    this.setupRefreshTimer();
  }

  private setupRefreshTimer(): void {
    this.clearRefreshTimer();
    if (this.refreshInterval > 0) {
      this.refreshTimer = setInterval(() => {
        this.refreshAllData();
      }, this.refreshInterval * 1000);
    }
  }

  private clearRefreshTimer(): void {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
      this.refreshTimer = null;
    }
  }

  // SVG Chart Coordinate Helpers
  get selectedNode(): Node | undefined {
    return this.nodes().find(n => n.id === this.selectedNodeId());
  }

  get chartPointsCpu(): string {
    const data = this.metrics();
    if (!data || data.length === 0) return '';
    return this.generateSvgPath(data.map(m => m.cpuUtilizationPercent));
  }

  get chartPointsMemory(): string {
    const data = this.metrics();
    if (!data || data.length === 0) return '';
    return this.generateSvgPath(data.map(m => m.memoryUtilizationPercent));
  }

  private generateSvgPath(values: number[]): string {
    const width = 800;
    const height = 240;
    const padding = 20;

    if (values.length === 1) {
      const y = height - padding - (values[0] / 100.0) * (height - 2 * padding);
      return `${padding},${y} ${width - padding},${y}`;
    }

    const stepX = (width - 2 * padding) / (values.length - 1);
    return values
      .map((val, idx) => {
        const x = padding + idx * stepX;
        const clamped = Math.max(0, Math.min(100, val));
        const y = height - padding - (clamped / 100.0) * (height - 2 * padding);
        return `${Math.round(x)},${Math.round(y)}`;
      })
      .join(' ');
  }
}
