import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NodeService } from '../../../core/services/node.service';
import { Node, NodeStatus, RegisterNodeRequest } from '../../../core/models/infrastructure.models';

@Component({
  selector: 'app-node-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './node-list.component.html',
  styleUrls: ['./node-list.component.scss']
})
export class NodeListComponent implements OnInit {
  private nodeService = inject(NodeService);
  private fb = inject(FormBuilder);

  nodes = signal<Node[]>([]);
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  // Filter
  statusFilter = signal<string>('ALL');

  // Modal State
  showRegisterModal = signal<boolean>(false);
  registerForm: FormGroup;
  isSubmitting = signal<boolean>(false);

  constructor() {
    this.registerForm = this.fb.group({
      hostname: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      ipAddress: ['192.168.1.100', [Validators.required]],
      totalCpuCores: [8, [Validators.required, Validators.min(1), Validators.max(128)]],
      totalMemoryMb: [16384, [Validators.required, Validators.min(512), Validators.max(1048576)]],
      simulated: [true]
    });
  }

  ngOnInit(): void {
    this.fetchNodes();
  }

  fetchNodes(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.nodeService.getNodes().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.nodes.set(res.data);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to retrieve cluster nodes');
        this.isLoading.set(false);
      }
    });
  }

  get filteredNodes(): Node[] {
    const filter = this.statusFilter();
    if (filter === 'ALL') return this.nodes();
    return this.nodes().filter(n => n.status === filter);
  }

  get totalCpuCapacity(): number {
    return this.nodes().reduce((sum, n) => sum + n.totalCpuCores, 0);
  }

  get totalAllocatedCpu(): number {
    return this.nodes().reduce((sum, n) => sum + n.allocatedCpuCores, 0);
  }

  get totalMemoryGb(): number {
    return Math.round(this.nodes().reduce((sum, n) => sum + n.totalMemoryMb, 0) / 1024);
  }

  get totalAllocatedMemoryGb(): number {
    return Math.round(this.nodes().reduce((sum, n) => sum + n.allocatedMemoryMb, 0) / 1024);
  }

  openRegisterModal(): void {
    this.registerForm.reset({
      hostname: `worker-node-${Math.floor(100 + Math.random() * 900)}`,
      ipAddress: `192.168.1.${Math.floor(10 + Math.random() * 200)}`,
      totalCpuCores: 8,
      totalMemoryMb: 16384,
      simulated: true
    });
    this.showRegisterModal.set(true);
  }

  closeRegisterModal(): void {
    this.showRegisterModal.set(false);
  }

  submitRegister(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const req: RegisterNodeRequest = this.registerForm.value;
    this.nodeService.registerNode(req).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.closeRegisterModal();
        this.showFlashMessage(`Node '${res.data.hostname}' registered successfully.`);
        this.fetchNodes();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to register node');
      }
    });
  }

  updateStatus(nodeId: string, status: NodeStatus): void {
    this.nodeService.updateNodeStatus(nodeId, status).subscribe({
      next: (res) => {
        this.showFlashMessage(`Host '${res.data.hostname}' transitioned to ${status}.`);
        this.fetchNodes();
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || `Failed to transition host to ${status}`);
      }
    });
  }

  deleteNode(node: Node): void {
    if (!confirm(`Are you sure you want to decommission and remove node '${node.hostname}'?`)) {
      return;
    }

    this.nodeService.deleteNode(node.id).subscribe({
      next: () => {
        this.showFlashMessage(`Host '${node.hostname}' decommissioned and removed.`);
        this.fetchNodes();
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || `Failed to decommission host '${node.hostname}'`);
      }
    });
  }

  private showFlashMessage(msg: string): void {
    this.successMessage.set(msg);
    setTimeout(() => this.successMessage.set(null), 4000);
  }
}
