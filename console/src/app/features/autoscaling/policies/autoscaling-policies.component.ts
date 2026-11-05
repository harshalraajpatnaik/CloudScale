import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { AutoscalingService } from '../../../core/services/autoscaling.service';
import { WorkloadService } from '../../../core/services/workload.service';
import { CreateScalingPolicyRequest, ScalingEvaluationResult, ScalingEvent, ScalingPolicy } from '../../../core/models/monitoring.models';
import { Workload } from '../../../core/models/infrastructure.models';

@Component({
  selector: 'app-autoscaling-policies',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './autoscaling-policies.component.html',
  styleUrls: ['./autoscaling-policies.component.scss']
})
export class AutoscalingPoliciesComponent implements OnInit {
  private autoscalingService = inject(AutoscalingService);
  private workloadService = inject(WorkloadService);
  private fb = inject(FormBuilder);

  policies = signal<ScalingPolicy[]>([]);
  workloads = signal<Workload[]>([]);
  scalingEvents = signal<ScalingEvent[]>([]);

  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  // Modal State
  showCreateModal = signal<boolean>(false);
  createForm: FormGroup;
  isSubmitting = signal<boolean>(false);

  // Evaluation Result Modal / Banner
  evaluationResult = signal<ScalingEvaluationResult | null>(null);
  isEvaluating = signal<boolean>(false);
  simulatedCpuInput = 85.0;

  constructor() {
    this.createForm = this.fb.group({
      workloadId: ['', [Validators.required]],
      cpuScaleUpThreshold: [80, [Validators.required, Validators.min(10), Validators.max(100)]],
      cpuScaleDownThreshold: [30, [Validators.required, Validators.min(5), Validators.max(90)]],
      minReplicas: [1, [Validators.required, Validators.min(1)]],
      maxReplicas: [10, [Validators.required, Validators.min(1)]],
      cooldownSeconds: [300, [Validators.required, Validators.min(10)]],
      evaluationWindowSeconds: [300, [Validators.required, Validators.min(30)]]
    });
  }

  ngOnInit(): void {
    this.fetchData();
  }

  fetchData(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.autoscalingService.getPolicies().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.policies.set(res.data);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to retrieve scaling policies');
        this.isLoading.set(false);
      }
    });

    this.workloadService.getWorkloads().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.workloads.set(res.data);
          if (res.data.length > 0) {
            // Load events for the first workload
            this.fetchEventsForWorkload(res.data[0].id);
          }
        }
      }
    });
  }

  fetchEventsForWorkload(workloadId: string): void {
    this.autoscalingService.getScalingEvents(workloadId).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.scalingEvents.set(res.data);
        }
      }
    });
  }

  openCreateModal(): void {
    this.createForm.reset({
      workloadId: this.workloads().length > 0 ? this.workloads()[0].id : '',
      cpuScaleUpThreshold: 80,
      cpuScaleDownThreshold: 30,
      minReplicas: 1,
      maxReplicas: 10,
      cooldownSeconds: 300,
      evaluationWindowSeconds: 300
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

    const req: CreateScalingPolicyRequest = this.createForm.value;
    this.autoscalingService.createPolicy(req).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.closeCreateModal();
        this.showFlashMessage(`Autoscaling policy created for workload '${res.data.workloadName}'.`);
        this.fetchData();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to create autoscaling policy');
      }
    });
  }

  evaluatePolicy(policy: ScalingPolicy): void {
    this.isEvaluating.set(true);
    this.evaluationResult.set(null);

    this.autoscalingService.evaluatePolicy(policy.id, this.simulatedCpuInput).subscribe({
      next: (res) => {
        this.isEvaluating.set(false);
        if (res.success && res.data) {
          this.evaluationResult.set(res.data);
          this.showFlashMessage(`Policy evaluated: Decision -> ${res.data.decision}`);
          this.fetchData();
        }
      },
      error: (err) => {
        this.isEvaluating.set(false);
        this.errorMessage.set(err.error?.message || 'Policy evaluation failed');
      }
    });
  }

  deletePolicy(policy: ScalingPolicy): void {
    if (!confirm(`Are you sure you want to delete the scaling policy for '${policy.workloadName}'?`)) {
      return;
    }

    this.autoscalingService.deletePolicy(policy.id).subscribe({
      next: () => {
        this.showFlashMessage(`Scaling policy removed.`);
        this.fetchData();
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to delete policy');
      }
    });
  }

  dismissEvaluationResult(): void {
    this.evaluationResult.set(null);
  }

  private showFlashMessage(msg: string): void {
    this.successMessage.set(msg);
    setTimeout(() => this.successMessage.set(null), 4500);
  }
}
