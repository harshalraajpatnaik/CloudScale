from fastapi import APIRouter
from pydantic import BaseModel
from typing import List, Dict, Any, Optional
from app.services.forecasting import CapacityForecaster

router = APIRouter()
forecaster = CapacityForecaster()

class ForecastRequest(BaseModel):
    history: List[Dict[str, Any]]
    metricKey: Optional[str] = "cpuUtilizationPercent"
    horizonSteps: Optional[int] = 12

@router.post("/capacity")
async def generate_capacity_forecast(req: ForecastRequest):
    forecaster.forecast_horizon_steps = req.horizonSteps or 12
    result = forecaster.forecast_capacity(req.history, req.metricKey or "cpuUtilizationPercent")
    return {
        "success": True,
        "data": result
    }
