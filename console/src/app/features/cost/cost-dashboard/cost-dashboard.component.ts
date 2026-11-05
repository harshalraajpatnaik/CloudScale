import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CostService } from '../../../core/services/cost.service';
import { CostSummary, NodeCost, WasteAnalysis, WorkloadCost } from '../../../core/models/cost.models';

@Component({
  selector: 'app-cost-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './cost-dashboard.component.html',
  styleUrls: ['./cost-dashboard.component.scss']
})
export class CostDashboardComponent implements OnInit {
  private costService = inject(CostService);

  summary = signal<CostSummary | null>(null);
  waste = signal<WasteAnalysis | null>(null);
  nodeCosts = signal<NodeCost[]>([]);
  workloadCosts = signal<WorkloadCost[]>([]);

  activeView = signal<'nodes' | 'workloads'>('nodes');
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  searchFilter = signal<string>('');

  ngOnInit(): void {
    this.fetchCostData();
  }

  fetchCostData(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.costService.getSummary().subscribe({
      next: (data) => this.summary.set(data),
      error: (err) => this.errorMessage.set(err?.error?.error || 'Failed to load spend summary.')
    });

    this.costService.getWasteAnalysis().subscribe({
      next: (data) => this.waste.set(data),
      error: () => {}
    });

    this.costService.getNodeCosts().subscribe({
      next: (data) => {
        this.nodeCosts.set(data || []);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });

    this.costService.getWorkloadCosts().subscribe({
      next: (data) => this.workloadCosts.set(data || []),
      error: () => {}
    });
  }

  filteredWorkloads(): WorkloadCost[] {
    const q = this.searchFilter().toLowerCase().trim();
    if (!q) return this.workloadCosts();
    return this.workloadCosts().filter(w =>
      w.name.toLowerCase().includes(q) ||
      w.assignedNode.toLowerCase().includes(q) ||
      w.priority.toLowerCase().includes(q)
    );
  }
}
