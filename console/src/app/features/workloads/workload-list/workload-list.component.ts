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
  isSchedulingAll = signal<boolean>(false);

  constructor() {
    this.createForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      workloadType: ['SERVICE', [Validators.required]],
      priority: ['NORMAL', [Validators.required]],
      cpuCoresRequired: [2, [Validators.required, Validators.min(1), Validators.max(32)]],
      memoryMbRequired: [2048, [Validators.required, Validators.min(128), Validators.max(65536)]],
      schedulingAlgorithm: ['RESOURCE_AWARE'],
      autoSchedule: [true]
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
      memoryMbRequired: 2048,
      schedulingAlgorithm: this.selectedAlgorithm,
      autoSchedule: true
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

    const formVal = this.createForm.value;
    const req: CreateWorkloadRequest = {
      name: formVal.name,
      workloadType: formVal.workloadType,
      priority: formVal.priority,
      cpuCoresRequired: formVal.cpuCoresRequired,
      memoryMbRequired: formVal.memoryMbRequired
    };
    const algo: SchedulingAlgorithmType = formVal.schedulingAlgorithm || this.selectedAlgorithm;
    const autoSchedule: boolean = formVal.autoSchedule !== false;

    this.workloadService.createWorkload(req).subscribe({
      next: (res) => {
        const createdWorkload = res.data;
        if (autoSchedule) {
          this.workloadService.scheduleWorkload(createdWorkload.id, algo).subscribe({
            next: (schedRes) => {
              this.isSubmitting.set(false);
              this.closeCreateModal();
              this.showFlashMessage(`Workload '${schedRes.data.name}' deployed and scheduled onto '${schedRes.data.assignedNodeHostname}' via ${algo}.`);
              this.fetchWorkloads();
            },
            error: (schedErr) => {
              this.isSubmitting.set(false);
              this.closeCreateModal();
              this.showFlashMessage(`Workload '${createdWorkload.name}' created, but placement failed: ${schedErr.error?.error?.message || 'Insufficient node capacity'}.`);
              this.fetchWorkloads();
            }
          });
        } else {
          this.isSubmitting.set(false);
          this.closeCreateModal();
          this.showFlashMessage(`Workload '${createdWorkload.name}' submitted in PENDING state.`);
          this.fetchWorkloads();
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err.error?.error?.message || err.error?.message || 'Failed to submit workload');
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
        this.errorMessage.set(err.error?.error?.message || err.error?.message || `Placement failed: Insufficient node capacity.`);
        this.fetchWorkloads();
      }
    });
  }

  scheduleAllPending(): void {
    const pendings = this.workloads().filter(w => w.actualState === 'PENDING' || w.actualState === 'FAILED');
    if (pendings.length === 0) return;

    this.isSchedulingAll.set(true);
    let completed = 0;
    let failed = 0;

    const processNext = (index: number) => {
      if (index >= pendings.length) {
        this.isSchedulingAll.set(false);
        this.showFlashMessage(`Bulk scheduling finished: ${completed} scheduled successfully, ${failed} failed.`);
        this.fetchWorkloads();
        return;
      }

      const w = pendings[index];
      this.workloadService.scheduleWorkload(w.id, this.selectedAlgorithm).subscribe({
        next: () => {
          completed++;
          processNext(index + 1);
        },
        error: () => {
          failed++;
          processNext(index + 1);
        }
      });
    };

    processNext(0);
  }

  stopWorkload(workload: Workload): void {
    this.workloadService.stopWorkload(workload.id).subscribe({
      next: (res) => {
        this.showFlashMessage(`Workload '${res.data.name}' stopped and capacity released.`);
        this.fetchWorkloads();
      },
      error: (err) => {
        this.errorMessage.set(err.error?.error?.message || err.error?.message || 'Failed to stop workload');
      }
    });
  }

  restartWorkload(workload: Workload): void {
    const algo = this.selectedAlgorithm;
    this.workloadService.restartWorkload(workload.id, algo).subscribe({
      next: (res) => {
        this.showFlashMessage(`Workload '${res.data.name}' restarted and scheduled onto '${res.data.assignedNodeHostname || 'optimal host'}' via ${algo}.`);
        this.fetchWorkloads();
      },
      error: (err) => {
        this.errorMessage.set(err.error?.error?.message || err.error?.message || 'Failed to restart workload');
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
        this.errorMessage.set(err.error?.error?.message || err.error?.message || 'Failed to suspend workload');
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
