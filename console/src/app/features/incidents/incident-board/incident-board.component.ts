import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AlertService } from '../../../core/services/alert.service';
import { ToastService } from '../../../core/services/toast.service';
import { Alert, AlertMetrics } from '../../../core/models/alert.models';

@Component({
  selector: 'app-incident-board',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './incident-board.component.html',
  styleUrls: ['./incident-board.component.scss']
})
export class IncidentBoardComponent implements OnInit {
  private alertService = inject(AlertService);
  private toastService = inject(ToastService);

  alerts = signal<Alert[]>([]);
  metrics = signal<AlertMetrics | null>(null);

  selectedStatus = signal<string>('ACTIVE');
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.fetchAlerts();
    this.fetchMetrics();
  }

  fetchAlerts(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.alertService.listAlerts(this.selectedStatus()).subscribe({
      next: (data) => {
        this.alerts.set(data || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        const msg = err?.error?.error?.message || err?.error?.message || 'Failed to load alerts.';
        this.errorMessage.set(msg);
        this.isLoading.set(false);
      }
    });
  }

  fetchMetrics(): void {
    this.alertService.getMetrics().subscribe({
      next: (data) => this.metrics.set(data),
      error: () => {}
    });
  }

  onStatusFilterChange(status: string): void {
    this.selectedStatus.set(status);
    this.fetchAlerts();
  }

  resolveAlert(alert: Alert): void {
    this.alertService.resolveAlert(alert.id).subscribe({
      next: (resolved) => {
        if (this.selectedStatus() === 'ACTIVE') {
          this.alerts.update(list => list.filter(a => a.id !== resolved.id));
        } else {
          this.alerts.update(list => list.map(a => a.id === resolved.id ? resolved : a));
        }
        this.toastService.success(`Incident '${alert.alertType}' successfully marked as RESOLVED.`);
        this.fetchMetrics();
      },
      error: (err) => {
        const msg = err?.error?.error?.message || err?.error?.message || 'Failed to resolve alert.';
        this.toastService.error(msg);
      }
    });
  }

  simulateIncident(): void {
    this.alertService.simulateAlert({
      severity: 'CRITICAL',
      alertType: 'HOST_SATURATION_SPIKE',
      message: 'Compute node CPU core saturation exceeded 95% threshold for >3 consecutive evaluation windows.'
    }).subscribe({
      next: (alert) => {
        if (this.selectedStatus() !== 'RESOLVED') {
          this.alerts.update(list => [alert, ...list]);
        }
        this.toastService.info('Simulated incident alert triggered.');
        this.fetchMetrics();
      },
      error: (err) => {
        const msg = err?.error?.error?.message || err?.error?.message || 'Failed to trigger simulated alert.';
        this.toastService.error(msg);
      }
    });
  }
}
