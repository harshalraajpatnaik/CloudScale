import os
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PROJECT_NAME: str = "CloudScale AI & Intelligence Service"
    API_V1_STR: str = "/api/v1"
    PORT: int = int(os.getenv("AI_SERVICE_PORT", "8000"))
    HOST: str = os.getenv("AI_SERVICE_HOST", "0.0.0.0")
    
    # Gemini Integration
    GEMINI_API_KEY: str = os.getenv("GEMINI_API_KEY", "")
    GEMINI_MODEL: str = os.getenv("GEMINI_MODEL", "gemini-3.5-flash-lite")

    # Control Plane URL
    CONTROL_PLANE_URL: str = os.getenv("CONTROL_PLANE_URL", "http://localhost:8080")

    class Config:
        case_sensitive = True

settings = Settings()
