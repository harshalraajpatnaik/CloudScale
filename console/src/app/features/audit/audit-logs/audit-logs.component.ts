import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditService } from '../../../core/services/audit.service';
import { AuditLog } from '../../../core/models/audit.models';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit-logs.component.html',
  styleUrls: ['./audit-logs.component.scss']
})
export class AuditLogsComponent implements OnInit {
  private auditService = inject(AuditService);

  logs = signal<AuditLog[]>([]);
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  searchFilter = signal<string>('');

  ngOnInit(): void {
    this.fetchLogs();
  }

  fetchLogs(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.auditService.getRecentLogs(50).subscribe({
      next: (data) => {
        this.logs.set(data || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.error || 'Failed to load audit trail.');
        this.isLoading.set(false);
      }
    });
  }

  filteredLogs(): AuditLog[] {
    const q = this.searchFilter().toLowerCase().trim();
    if (!q) return this.logs();
    return this.logs().filter(log =>
      log.action.toLowerCase().includes(q) ||
      log.resourceType.toLowerCase().includes(q) ||
      log.actorEmail.toLowerCase().includes(q) ||
      log.resourceId.toLowerCase().includes(q)
    );
  }
}
