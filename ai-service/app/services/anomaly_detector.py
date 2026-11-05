import numpy as np
import pandas as pd
from typing import Dict, Any, List
from sklearn.ensemble import IsolationForest

class AnomalyDetector:
    """
    Multivariate anomaly detection engine using Z-score and Isolation Forest.
    """

    def __init__(self, contamination: float = 0.05):
        self.contamination = contamination

    def detect_anomalies(self, telemetry_series: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        if len(telemetry_series) < 5:
            return []

        df = pd.DataFrame(telemetry_series)

        # Standardize metric keys
        cpu_col = "cpuUtilizationPercent" if "cpuUtilizationPercent" in df.columns else "cpu_utilization_percent"
        mem_col = "memoryUtilizationPercent" if "memoryUtilizationPercent" in df.columns else "memory_utilization_percent"
        ts_col = "sampledAt" if "sampledAt" in df.columns else ("timestamp" if "timestamp" in df.columns else None)

        if cpu_col not in df.columns:
            return []

        anomalies = []

        # 1. Z-Score Univariate Detection on CPU & Memory
        cpu_vals = df[cpu_col].astype(float).values
        cpu_mean = np.mean(cpu_vals)
        cpu_std = np.std(cpu_vals)

        if cpu_std > 0.001:
            z_scores_cpu = (cpu_vals - cpu_mean) / cpu_std
        else:
            z_scores_cpu = np.zeros_like(cpu_vals)

        for i, z in enumerate(z_scores_cpu):
            if abs(z) >= 2.5:
                val = float(cpu_vals[i])
                ts = df.iloc[i][ts_col] if ts_col else None
                severity = "CRITICAL" if abs(z) >= 3.5 or val >= 92.0 else "WARNING"
                anomalies.append({
                    "timestamp": str(ts) if ts else None,
                    "detector": "Z-Score",
                    "metric": "CPU Saturation",
                    "observedValue": round(val, 2),
                    "expectedMean": round(float(cpu_mean), 2),
                    "anomalyScore": round(float(abs(z)), 2),
                    "severity": severity,
                    "description": f"CPU saturation of {val}% deviates {round(abs(z), 1)} standard deviations from rolling baseline ({round(cpu_mean, 1)}%)."
                })

        # 2. Multivariate Isolation Forest (CPU + Memory + Workloads)
        features = [cpu_col]
        if mem_col in df.columns:
            features.append(mem_col)

        if len(df) >= 10:
            try:
                X = df[features].astype(float).values
                iso = IsolationForest(contamination=self.contamination, random_state=42)
                preds = iso.fit_predict(X)
                scores = iso.decision_function(X)

                for i, pred in enumerate(preds):
                    if pred == -1: # outlier detected
                        ts = df.iloc[i][ts_col] if ts_col else None
                        anomalies.append({
                            "timestamp": str(ts) if ts else None,
                            "detector": "Isolation Forest",
                            "metric": "Multivariate Saturation Vector",
                            "observedValue": round(float(df.iloc[i][cpu_col]), 2),
                            "expectedMean": round(float(cpu_mean), 2),
                            "anomalyScore": round(float(abs(scores[i])), 3),
                            "severity": "WARNING",
                            "description": f"Multivariate outlier detected: uncharacteristic correlation between CPU ({df.iloc[i][cpu_col]}%) and Memory."
                        })
            except Exception:
                pass

        return anomalies
