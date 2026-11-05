import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'nodes'
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'nodes',
    canActivate: [authGuard],
    loadComponent: () => import('./features/infrastructure/node-list/node-list.component').then(m => m.NodeListComponent)
  },
  {
    path: 'workloads',
    canActivate: [authGuard],
    loadComponent: () => import('./features/workloads/workload-list/workload-list.component').then(m => m.WorkloadListComponent)
  },
  {
    path: 'metrics',
    canActivate: [authGuard],
    loadComponent: () => import('./features/monitoring/metrics-dashboard/metrics-dashboard.component').then(m => m.MetricsDashboardComponent)
  },
  {
    path: 'autoscaling',
    canActivate: [authGuard],
    loadComponent: () => import('./features/autoscaling/policies/autoscaling-policies.component').then(m => m.AutoscalingPoliciesComponent)
  },
  {
    path: 'ai-assistant',
    canActivate: [authGuard],
    loadComponent: () => import('./features/ai/assistant/ai-assistant.component').then(m => m.AiAssistantComponent)
  },
  {
    path: 'health',
    loadComponent: () => import('./features/health/health.component').then(m => m.HealthComponent)
  },
  {
    path: '**',
    redirectTo: 'nodes'
  }
];
