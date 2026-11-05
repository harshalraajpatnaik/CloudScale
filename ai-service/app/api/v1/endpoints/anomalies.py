from fastapi import APIRouter
from pydantic import BaseModel
from typing import List, Dict, Any
from app.services.anomaly_detector import AnomalyDetector

router = APIRouter()
detector = AnomalyDetector()

class AnomalyRequest(BaseModel):
    telemetry: List[Dict[str, Any]]

@router.post("/detect")
async def detect_anomalies(req: AnomalyRequest):
    anomalies = detector.detect_anomalies(req.telemetry)
    return {
        "success": True,
        "count": len(anomalies),
        "data": anomalies
    }
