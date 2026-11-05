import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'health'
  },
  {
    path: 'health',
    loadComponent: () => import('./features/health/health.component').then(m => m.HealthComponent)
  }
];
