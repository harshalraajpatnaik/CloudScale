export interface AIRecommendation {
  id: string;
  category: string;
  explanation: string;
  affectedWorkloadIds: string[];
  confidenceScore: number;
  expectedImpact: string;
  suggestedAction: string;
  actionPayload: Record<string, any>;
  approvalStatus: 'PENDING' | 'APPROVED' | 'REJECTED';
  approvedByUserId?: string;
  createdAt: string;
}

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

export interface ChatRequest {
  message: string;
  history?: Array<{ role: string; content: string }>;
}

export interface ChatResponse {
  reply: string;
}

export interface DiagnoseIncidentResponse {
  title: string;
  rootCause: string;
  severity: string;
  affectedComponents: string[];
  remediationSteps: string[];
  confidenceScore: number;
}

export interface ForecastModelScore {
  model: string;
  mae: number;
  rmse: number;
}

export interface ForecastDataPoint {
  step: number;
  timestamp: string;
  forecastedUtilization: number;
  lowerConfidence: number;
  upperConfidence: number;
}

export interface ForecastResponse {
  evaluatedModels: ForecastModelScore[];
  selectedModel: string;
  modelPerformance: Record<string, any>;
  forecastHorizonMinutes: number;
  forecast: ForecastDataPoint[];
}
