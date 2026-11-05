import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { catchError, of } from 'rxjs';

interface ServiceHealth {
  name: string;
  url: string;
  status: 'CHECKING' | 'ONLINE' | 'OFFLINE';
  details?: any;
  error?: string;
}

@Component({
  selector: 'app-health',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './health.component.html',
  styleUrl: './health.component.scss'
})
export class HealthComponent implements OnInit {
  private http = inject(HttpClient);

  services = signal<ServiceHealth[]>([
    { name: 'Java Spring Boot Control Plane', url: '/api/v1/health', status: 'CHECKING' },
    { name: 'Python FastAPI AI Service', url: 'http://localhost:8000/health', status: 'CHECKING' },
    { name: 'Simulated Node Agent', url: 'http://localhost:8090/health', status: 'CHECKING' }
  ]);

  ngOnInit() {
    this.checkAllServices();
  }

  checkAllServices() {
    this.services.update(list => list.map(s => ({ ...s, status: 'CHECKING', error: undefined })));

    this.services().forEach((service, index) => {
      this.http.get(service.url).pipe(
        catchError(err => of({ error: err.message || 'Connection failed' }))
      ).subscribe(res => {
        this.services.update(list => {
          const updated = [...list];
          if (res && !(res as any).error) {
            updated[index] = { ...service, status: 'ONLINE', details: res };
          } else {
            updated[index] = { ...service, status: 'OFFLINE', error: (res as any)?.error || 'Service unreachable' };
          }
          return updated;
        });
      });
    });
  }
}
