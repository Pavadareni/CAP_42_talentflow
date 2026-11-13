from datetime import date

from fastapi import APIRouter, HTTPException, Query

from app.services.allocation_data_service import (
    allocation_data_service
)

from app.services.eligibility_engine import eligibility_engine

router = APIRouter(
    prefix="/eligibility",
    tags=["Employee Eligibility"]
)

@router.get("/project/{project_id}")
def evaluate_project_eligibility(
    project_id: int,
    requested_hours: float = Query(default=20, gt=0)
):

    try:

        data = allocation_data_service.load_project_data(project_id)

        projects_df = data["projects_df"]

        if projects_df.empty:
            raise HTTPException(
                status_code=404,
                detail="Project not found"
            )

        project = projects_df.iloc[0]

        start_date = date.fromisoformat(
            str(project["startDate"])[:10]
        )

        end_date = date.fromisoformat(
            str(project["endDate"])[:10]
        )

        results = eligibility_engine.evaluate_candidates(
            employees_df=data["employees_df"],
            employee_skills_df=data["employee_skills_df"],
            project_requirements_df=data["project_requirements_df"],
            availability_df=data["availability_df"],
            existing_allocations_df=data["existing_allocations_df"],
            start_date=start_date,
            end_date=end_date,
            requested_hours=requested_hours
        )

        return {
            "status": "success",
            "projectId": project_id,
            "requestedHoursPerWeek": requested_hours,
            "totalEmployees": len(results),
            "eligibleCount": int(results["eligible"].sum()),
            "rejectedCount": int((~results["eligible"]).sum()),
            "results": results.to_dict(orient="records")
        }

    except HTTPException:
        raise

    except Exception as error:
        raise HTTPException(
            status_code=502,
            detail=str(error)
        )