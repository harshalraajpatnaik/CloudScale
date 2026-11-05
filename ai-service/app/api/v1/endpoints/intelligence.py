from fastapi import APIRouter
from pydantic import BaseModel
from typing import List, Dict, Any, Optional
from app.services.llm_provider import get_llm_provider

router = APIRouter()

class DiagnoseRequest(BaseModel):
    incidentData: Dict[str, Any]

class RecommendRequest(BaseModel):
    clusterContext: Dict[str, Any]

class ChatRequest(BaseModel):
    message: str
    history: Optional[List[Dict[str, str]]] = []
    clusterContext: Optional[Dict[str, Any]] = {}

@router.post("/diagnose")
async def diagnose_incident(req: DiagnoseRequest):
    provider = get_llm_provider()
    report = await provider.diagnose_incident(req.incidentData)
    return {
        "success": True,
        "data": report
    }

@router.post("/recommend")
async def generate_recommendations(req: RecommendRequest):
    provider = get_llm_provider()
    recommendations = await provider.generate_recommendations(req.clusterContext)
    return {
        "success": True,
        "count": len(recommendations),
        "data": recommendations
    }

@router.post("/chat")
async def chat_assistant(req: ChatRequest):
    provider = get_llm_provider()
    reply = await provider.chat(req.message, req.history or [], req.clusterContext or {})
    return {
        "success": True,
        "reply": reply
    }
