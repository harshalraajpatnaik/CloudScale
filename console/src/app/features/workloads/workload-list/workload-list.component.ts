import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { WorkloadService } from '../../../core/services/workload.service';
import { CreateWorkloadRequest, SchedulingAlgorithmType, Workload, WorkloadPriority, WorkloadType } from '../../../core/models/infrastructure.models';

@Component({
  selector: 'app-workload-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './workload-list.component.html',
  styleUrls: ['./workload-list.component.scss']
})
export class WorkloadListComponent implements OnInit {
  private workloadService = inject(WorkloadService);
  private fb = inject(FormBuilder);

  workloads = signal<Workload[]>([]);
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  // Filters & Scheduling
  stateFilter = signal<string>('ALL');
  selectedAlgorithm: SchedulingAlgorithmType = 'RESOURCE_AWARE';

  // Modal State
  showCreateModal = signal<boolean>(false);
  createForm: FormGroup;
  isSubmitting = signal<boolean>(false);

  // Selected workload for scheduling dialog
  schedulingWorkloadId = signal<string | null>(null);

  constructor() {
    this.createForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      workloadType: ['SERVICE', [Validators.required]],
      priority: ['NORMAL', [Validators.required]],
      cpuCoresRequired: [2, [Validators.required, Validators.min(1), Validators.max(32)]],
      memoryMbRequired: [2048, [Validators.required, Validators.min(128), Validators.max(65536)]]
    });
  }

  ngOnInit(): void {
    this.fetchWorkloads();
  }

  fetchWorkloads(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.workloadService.getWorkloads().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.workloads.set(res.data);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to retrieve workloads');
        this.isLoading.set(false);
      }
    });
  }

  get filteredWorkloads(): Workload[] {
    const filter = this.stateFilter();
    if (filter === 'ALL') return this.workloads();
    return this.workloads().filter(w => w.actualState === filter);
  }

  get runningCount(): number {
    return this.workloads().filter(w => w.actualState === 'RUNNING').length;
  }

  get pendingCount(): number {
    return this.workloads().filter(w => w.actualState === 'PENDING').length;
  }

  get failedCount(): number {
    return this.workloads().filter(w => w.actualState === 'FAILED').length;
  }

  openCreateModal(): void {
    this.createForm.reset({
      name: `workload-${Math.floor(100 + Math.random() * 900)}`,
      workloadType: 'SERVICE',
      priority: 'NORMAL',
      cpuCoresRequired: 2,
      memoryMbRequired: 2048
    });
    this.showCreateModal.set(true);
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  submitCreate(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const req: CreateWorkloadRequest = this.createForm.value;
    this.workloadService.createWorkload(req).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.closeCreateModal();
        this.showFlashMessage(`Workload '${res.data.name}' submitted in PENDING state.`);
        this.fetchWorkloads();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to submit workload');
      }
    });
  }

  scheduleWorkload(workload: Workload, algorithm: SchedulingAlgorithmType): void {
    this.schedulingWorkloadId.set(workload.id);
    this.errorMessage.set(null);

    this.workloadService.scheduleWorkload(workload.id, algorithm).subscribe({
      next: (res) => {
        this.schedulingWorkloadId.set(null);
        this.showFlashMessage(`Workload '${res.data.name}' scheduled onto node '${res.data.assignedNodeHostname}' via ${algorithm}.`);
        this.fetchWorkloads();
      },
      error: (err) => {
        this.schedulingWorkloadId.set(null);
        this.errorMessage.set(err.error?.message || `Placement failed: Insufficient node capacity.`);
        this.fetchWorkloads();
      }
    });
  }

  stopWorkload(workload: Workload): void {
    this.workloadService.stopWorkload(workload.id).subscribe({
      next: (res) => {
        this.showFlashMessage(`Workload '${res.data.name}' stopped and capacity released.`);
        this.fetchWorkloads();
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to stop workload');
      }
    });
  }

  restartWorkload(workload: Workload): void {
    this.workloadService.restartWorkload(workload.id).subscribe({
      next: (res) => {
        this.showFlashMessage(`Workload '${res.data.name}' transitioned to PENDING for reschedule.`);
        this.fetchWorkloads();
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to restart workload');
      }
    });
  }

  suspendWorkload(workload: Workload): void {
    this.workloadService.suspendWorkload(workload.id).subscribe({
      next: (res) => {
        this.showFlashMessage(`Workload '${res.data.name}' suspended.`);
        this.fetchWorkloads();
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to suspend workload');
      }
    });
  }

  deleteWorkload(workload: Workload): void {
    if (!confirm(`Are you sure you want to delete workload '${workload.name}'?`)) {
      return;
    }

    this.workloadService.deleteWorkload(workload.id).subscribe({
      next: () => {
        this.showFlashMessage(`Workload '${workload.name}' deleted.`);
        this.fetchWorkloads();
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to delete workload');
      }
    });
  }

  private showFlashMessage(msg: string): void {
    this.successMessage.set(msg);
    setTimeout(() => this.successMessage.set(null), 4500);
  }
}
