import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiService } from '../../../core/services/ai.service';
import { NodeService } from '../../../core/services/node.service';
import {
  AIRecommendation,
  ChatMessage,
  DiagnoseIncidentResponse,
  ForecastResponse
} from '../../../core/models/ai.models';
import { Node } from '../../../core/models/infrastructure.models';

@Component({
  selector: 'app-ai-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-assistant.component.html',
  styleUrls: ['./ai-assistant.component.scss']
})
export class AiAssistantComponent implements OnInit {
  private aiService = inject(AiService);
  private nodeService = inject(NodeService);

  activeTab = signal<'chat' | 'recommendations' | 'forecasting' | 'diagnostics'>('chat');

  // Chat State
  messages = signal<ChatMessage[]>([
    {
      role: 'assistant',
      content: 'Hello! I am CloudScale Intelligence, powered by Google Gemini (gemini-3.5-flash-lite). I analyze your cluster compute density, detect utilization anomalies, and recommend autonomous rightsizing policies. How can I assist you with your infrastructure today?',
      timestamp: new Date()
    }
  ]);
  userInput = signal<string>('');
  isChatLoading = signal<boolean>(false);

  // Recommendations State
  recommendations = signal<AIRecommendation[]>([]);
  isRecsLoading = signal<boolean>(false);
  recSuccessMessage = signal<string | null>(null);

  // Forecasting State
  nodes = signal<Node[]>([]);
  selectedNodeId = signal<string | null>(null);
  selectedDuration = signal<number>(60);
  forecastData = signal<ForecastResponse | null>(null);
  isForecastLoading = signal<boolean>(false);

  // Diagnostics State
  diagnosticsResult = signal<DiagnoseIncidentResponse | null>(null);
  isDiagnosing = signal<boolean>(false);
  incidentType = signal<'CPU_SATURATION' | 'MEMORY_LEAK' | 'FAILING_WORKLOAD'>('CPU_SATURATION');

  errorMessage = signal<string | null>(null);

  quickPrompts = [
    'Analyze compute density across all cluster nodes',
    'Which workloads are over-provisioned and can be downscaled?',
    'Assess failure risk and recommend proactive mitigations',
    'Explain how diurnal cycles affect capacity allocation'
  ];

  ngOnInit(): void {
    this.fetchNodes();
    this.fetchRecommendations();
  }

  setTab(tab: 'chat' | 'recommendations' | 'forecasting' | 'diagnostics'): void {
    this.activeTab.set(tab);
    this.errorMessage.set(null);
  }

  // --- CHAT ACTIONS ---
  sendChat(overrideMessage?: string): void {
    const text = overrideMessage || this.userInput().trim();
    if (!text || this.isChatLoading()) return;

    const userMsg: ChatMessage = {
      role: 'user',
      content: text,
      timestamp: new Date()
    };

    this.messages.update(prev => [...prev, userMsg]);
    if (!overrideMessage) {
      this.userInput.set('');
    }
    this.isChatLoading.set(true);
    this.errorMessage.set(null);

    const historyPayload = this.messages().slice(0, -1).map(m => ({
      role: m.role,
      content: m.content
    }));

    this.aiService.chat({ message: text, history: historyPayload }).subscribe({
      next: (res) => {
        const assistantMsg: ChatMessage = {
          role: 'assistant',
          content: res.reply,
          timestamp: new Date()
        };
        this.messages.update(prev => [...prev, assistantMsg]);
        this.isChatLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.error || 'Failed to receive AI response from Gemini.');
        this.isChatLoading.set(false);
      }
    });
  }

  applyQuickPrompt(prompt: string): void {
    this.sendChat(prompt);
  }

  // --- RECOMMENDATION ACTIONS ---
  fetchRecommendations(): void {
    this.isRecsLoading.set(true);
    this.aiService.listRecommendations().subscribe({
      next: (data) => {
        this.recommendations.set(data || []);
        this.isRecsLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.error || 'Failed to load recommendations.');
        this.isRecsLoading.set(false);
      }
    });
  }

  scanAndGenerateRecommendations(): void {
    this.isRecsLoading.set(true);
    this.errorMessage.set(null);
    this.recSuccessMessage.set(null);

    this.aiService.generateRecommendations().subscribe({
      next: (data) => {
        this.recommendations.set(data || []);
        this.isRecsLoading.set(false);
        this.recSuccessMessage.set(`Successfully generated ${data.length} AI infrastructure recommendations using cluster telemetry.`);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.error || 'Failed to generate recommendations.');
        this.isRecsLoading.set(false);
      }
    });
  }

  approveRec(rec: AIRecommendation): void {
    this.aiService.approveRecommendation(rec.id).subscribe({
      next: (updated) => {
        this.recommendations.update(list => list.map(item => item.id === updated.id ? updated : item));
        this.recSuccessMessage.set(`Recommendation approved. Action '${updated.suggestedAction}' scheduled for execution.`);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.error || 'Failed to approve recommendation.');
      }
    });
  }

  rejectRec(rec: AIRecommendation): void {
    this.aiService.rejectRecommendation(rec.id).subscribe({
      next: (updated) => {
        this.recommendations.update(list => list.map(item => item.id === updated.id ? updated : item));
        this.recSuccessMessage.set(`Recommendation '${updated.id.substring(0, 8)}' rejected.`);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.error || 'Failed to reject recommendation.');
      }
    });
  }

  // --- FORECASTING ACTIONS ---
  fetchNodes(): void {
    this.nodeService.getNodes().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.nodes.set(res.data);
          if (res.data.length > 0 && !this.selectedNodeId()) {
            this.selectedNodeId.set(res.data[0].id);
            this.generateForecast();
          }
        }
      }
    });
  }

  onNodeSelected(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.selectedNodeId.set(select.value);
    this.generateForecast();
  }

  onDurationChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.selectedDuration.set(parseInt(select.value, 10));
    this.generateForecast();
  }

  generateForecast(): void {
    const nodeId = this.selectedNodeId();
    if (!nodeId) return;

    this.isForecastLoading.set(true);
    this.errorMessage.set(null);

    this.aiService.getNodeCapacityForecast(nodeId, this.selectedDuration()).subscribe({
      next: (data) => {
        this.forecastData.set(data);
        this.isForecastLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.error || 'Failed to generate capacity forecast.');
        this.isForecastLoading.set(false);
      }
    });
  }

  // --- DIAGNOSTICS ACTIONS ---
  runDiagnosis(): void {
    this.isDiagnosing.set(true);
    this.diagnosticsResult.set(null);
    this.errorMessage.set(null);

    const simulatedIncident: Record<string, any> = {
      type: this.incidentType(),
      triggeredAt: new Date().toISOString(),
      clusterMetrics: {
        avgCpuUtilization: this.incidentType() === 'CPU_SATURATION' ? 94.2 : 68.5,
        avgMemoryUtilization: this.incidentType() === 'MEMORY_LEAK' ? 96.8 : 54.0,
        unhealthyWorkloadCount: this.incidentType() === 'FAILING_WORKLOAD' ? 4 : 0
      },
      nodeHostname: this.nodes().length > 0 ? this.nodes()[0].hostname : 'worker-01'
    };

    this.aiService.diagnoseIncident(simulatedIncident).subscribe({
      next: (res) => {
        this.diagnosticsResult.set(res);
        this.isDiagnosing.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.error || 'Failed to run AI diagnosis.');
        this.isDiagnosing.set(false);
      }
    });
  }

  // --- SVG FORECAST PATH BUILDERS ---
  getForecastSvgPath(): string {
    const data = this.forecastData();
    if (!data || !data.forecast || data.forecast.length === 0) return '';

    const points = data.forecast;
    const width = 800;
    const height = 240;
    const padding = 20;

    const stepX = (width - 2 * padding) / Math.max(1, points.length - 1);

    return points.map((p, idx) => {
      const x = padding + idx * stepX;
      const y = height - padding - (Math.min(100, Math.max(0, p.forecastedUtilization)) / 100) * (height - 2 * padding);
      return `${idx === 0 ? 'M' : 'L'} ${x.toFixed(1)} ${y.toFixed(1)}`;
    }).join(' ');
  }

  getConfidenceCorridorSvgPath(): string {
    const data = this.forecastData();
    if (!data || !data.forecast || data.forecast.length === 0) return '';

    const points = data.forecast;
    const width = 800;
    const height = 240;
    const padding = 20;

    const stepX = (width - 2 * padding) / Math.max(1, points.length - 1);

    // Upper boundary
    const upperPoints = points.map((p, idx) => {
      const x = padding + idx * stepX;
      const y = height - padding - (Math.min(100, Math.max(0, p.upperConfidence)) / 100) * (height - 2 * padding);
      return `${idx === 0 ? 'M' : 'L'} ${x.toFixed(1)} ${y.toFixed(1)}`;
    }).join(' ');

    // Lower boundary (reversed)
    const lowerPoints = [...points].reverse().map((p, revIdx) => {
      const idx = points.length - 1 - revIdx;
      const x = padding + idx * stepX;
      const y = height - padding - (Math.min(100, Math.max(0, p.lowerConfidence)) / 100) * (height - 2 * padding);
      return `L ${x.toFixed(1)} ${y.toFixed(1)}`;
    }).join(' ');

    return `${upperPoints} ${lowerPoints} Z`;
  }
}
