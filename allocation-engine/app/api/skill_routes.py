from fastapi import APIRouter, HTTPException

from app.services.allocation_data_service import (
    allocation_data_service
)

from app.services.skill_matcher import skill_matching_engine

router = APIRouter(
    prefix="/skills",
    tags=["Skill Matching"]
)


@router.get("/project/{project_id}")
def match_project_skills(project_id: int):

    try:

        data = allocation_data_service.load_project_data(
            project_id
        )

        results = skill_matching_engine.match_all_employees(
            employees_df=data["employees_df"],
            employee_skills_df=data["employee_skills_df"],
            project_requirements_df=data["project_requirements_df"]
        )

        return {
            "status": "success",
            "projectId": project_id,
            "employeeCount": len(results),
            "results": results.to_dict(orient="records")
        }

    except Exception as error:

        raise HTTPException(
            status_code=502,
            detail=str(error)
        )
