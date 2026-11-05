from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.config import settings

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

@app.get("/health", tags=["Health"])
async def health_check():
    return {
        "status": "ok",
        "service": "cloudscale-ai-service",
        "version": "1.0.0",
        "gemini_configured": bool(settings.GEMINI_API_KEY)
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
