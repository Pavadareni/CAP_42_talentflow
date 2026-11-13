
from fastapi import APIRouter, HTTPException

from app.services.spring_api_client import spring_client

router = APIRouter(
    prefix="/backend",
    tags=["Spring Boot Integration"]
)


@router.get("/employees")
def get_employees_from_backend():

    try:
        employees = spring_client.get_employees()

        return {
            "status": "success",
            "source": "Spring Boot",
            "employees": employees
        }

    except RuntimeError as error:
        raise HTTPException(
            status_code=502,
            detail=str(error)
        )


@router.get("/projects/{project_id}")
def get_project_from_backend(project_id: int):

    try:
        project = spring_client.get_project(project_id)

        return {
            "status": "success",
            "source": "Spring Boot",
            "project": project
        }

    except RuntimeError as error:
        raise HTTPException(
            status_code=502,
            detail=str(error)
        )
