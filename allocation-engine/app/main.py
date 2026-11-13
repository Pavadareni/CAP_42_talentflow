
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.backend_routes import router as backend_router
from app.api.data_routes import router as data_router
from app.api.eligibility_routes import router as eligibility_router
from app.api.skill_routes import router as skill_router
from app.api.optimization_routes import router as optimization_router
from app.api.recommendation_routes import (
    router as recommendation_router
)

app = FastAPI(
    title="Employee Allocation Optimization Engine",
    description="Rule-based employee-to-project allocation recommendation service",
    version="1.0.0"
)

# CORS configuration for React frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "http://127.0.0.1:5173"
    ],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/")
def home():
    return {
        "message": "Employee Allocation Engine is running",
        "status": "success",
        "version": "1.0.0"
    }


@app.get("/health")
def health():
    return {
        "status": "healthy",
        "service": "Python Allocation Engine"
    }


# Register Spring Boot integration routes
app.include_router(backend_router)

# Register data loading routes
app.include_router(data_router)

# Register eligibility routes
app.include_router(eligibility_router)

# Register skill routes
app.include_router(skill_router)

# Register recommendation routes
app.include_router(recommendation_router)

# Register optimization routes
app.include_router(optimization_router)
