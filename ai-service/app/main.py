from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.config import settings
from app.api.v1.endpoints.forecast import router as forecast_router
from app.api.v1.endpoints.anomalies import router as anomalies_router
from app.api.v1.endpoints.intelligence import router as intelligence_router

app = FastAPI(
    title=settings.PROJECT_NAME,
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    docs_url=f"{settings.API_V1_STR}/docs"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(forecast_router, prefix=f"{settings.API_V1_STR}/forecast", tags=["Forecasting"])
app.include_router(anomalies_router, prefix=f"{settings.API_V1_STR}/anomalies", tags=["Anomalies"])
app.include_router(intelligence_router, prefix=f"{settings.API_V1_STR}/ai", tags=["Infrastructure Intelligence"])

@app.get("/health", tags=["Health"])
async def health_check():
    return {
        "status": "ok",
        "service": "cloudscale-ai-service",
        "version": "1.0.0",
        "gemini_configured": bool(settings.GEMINI_API_KEY),
        "model": settings.GEMINI_MODEL
    }

@app.get("/", tags=["Root"])
async def root():
    return {
        "message": "CloudScale AI & Intelligence Service",
        "docs": f"{settings.API_V1_STR}/docs"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=settings.HOST, port=settings.PORT, reload=True)
