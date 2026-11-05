import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
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

  // Search, Sort & Pagination State
  searchQuery = signal<string>('');
  sortColumn = signal<string>('sampledAt');
  sortDirection = signal<'asc' | 'desc'>('desc');
  currentPage = signal<number>(1);
  pageSize = signal<number>(15);

  // Filtered and Sorted Records
  filteredMetrics = computed(() => {
    let list = this.metrics();
    const query = this.searchQuery().trim().toLowerCase();
    if (query) {
      list = list.filter(m =>
        (m.hostname && m.hostname.toLowerCase().includes(query)) ||
        m.cpuUtilizationPercent.toString().includes(query) ||
        m.memoryUtilizationPercent.toString().includes(query) ||
        (m.activeWorkloadCount != null && m.activeWorkloadCount.toString().includes(query)) ||
        (m.sampledAt && m.sampledAt.toLowerCase().includes(query))
      );
    }

    const col = this.sortColumn();
    const dir = this.sortDirection() === 'asc' ? 1 : -1;

    return [...list].sort((a, b) => {
      let aVal: any = (a as any)[col];
      let bVal: any = (b as any)[col];

      if (col === 'sampledAt') {
        aVal = new Date(aVal || 0).getTime();
        bVal = new Date(bVal || 0).getTime();
      } else if (typeof aVal === 'string') {
        return aVal.localeCompare(bVal || '') * dir;
      } else {
        aVal = Number(aVal || 0);
        bVal = Number(bVal || 0);
      }
      return (aVal > bVal ? 1 : aVal < bVal ? -1 : 0) * dir;
    });
  });

  // Paginated Sliced Metrics
  paginatedMetrics = computed(() => {
    const list = this.filteredMetrics();
    const size = this.pageSize();
    if (size <= 0) return list;
    const start = (this.currentPage() - 1) * size;
    return list.slice(start, start + size);
  });

  selectedNodeWorkloadCount = computed(() => {
    const list = this.metrics();
    if (list.length > 0 && list[0].activeWorkloadCount != null) {
      return list[0].activeWorkloadCount;
    }
    const node = this.nodes().find(n => n.id === this.selectedNodeId());
    if (node && node.allocatedCpuCores) {
      return Math.max(1, Math.round(node.allocatedCpuCores / 2));
    }
    return 0;
  });

  totalPages = computed(() => {
    const size = this.pageSize();
    if (size <= 0) return 1;
    return Math.max(1, Math.ceil(this.filteredMetrics().length / size));
  });

  startIndex = computed(() => {
    if (this.filteredMetrics().length === 0) return 0;
    return (this.currentPage() - 1) * this.pageSize() + 1;
  });

  endIndex = computed(() => {
    const size = this.pageSize();
    if (size <= 0) return this.filteredMetrics().length;
    return Math.min(this.filteredMetrics().length, this.currentPage() * size);
  });

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
        this.errorMessage.set(err.error?.error?.message || err.error?.message || 'Failed to load compute nodes');
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
        this.errorMessage.set(err.error?.error?.message || err.error?.message || 'Failed to load telemetry samples');
        this.isLoading.set(false);
      }
    });
  }

  onNodeSelected(nodeId: string): void {
    this.selectedNodeId.set(nodeId);
    this.currentPage.set(1);
    this.fetchNodeMetrics(nodeId);
  }

  onDurationChange(duration: number): void {
    this.selectedDuration = duration;
    this.currentPage.set(1);
    this.refreshAllData();
  }

  onIntervalChange(interval: number): void {
    this.refreshInterval = interval;
    this.setupRefreshTimer();
  }

  // --- Sorting & Pagination Handlers ---
  onSort(column: string): void {
    if (this.sortColumn() === column) {
      this.sortDirection.set(this.sortDirection() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortColumn.set(column);
      this.sortDirection.set('desc');
    }
    this.currentPage.set(1);
  }

  setPage(page: number): void {
    if (page >= 1 && page <= this.totalPages()) {
      this.currentPage.set(page);
    }
  }

  onPageSizeChange(event: Event): void {
    const val = Number((event.target as HTMLSelectElement).value);
    this.pageSize.set(val);
    this.currentPage.set(1);
  }

  onSearchChange(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.searchQuery.set(val);
    this.currentPage.set(1);
  }

  clearSearch(): void {
    this.searchQuery.set('');
    this.currentPage.set(1);
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
