
from datetime import date

from fastapi import APIRouter, HTTPException, Query

from app.services.allocation_data_service import (
    allocation_data_service
)

from app.services.eligibility_engine import eligibility_engine

from app.services.skill_matcher import skill_matching_engine

from app.services.scoring_engine import scoring_engine


router = APIRouter(
    prefix="/recommendations",
    tags=["Employee Recommendations"]
)


@router.get("/project/{project_id}")
def get_project_recommendations(
    project_id: int,
    requested_hours: float = Query(default=20, gt=0)
):

    try:

        # 1. Load all project data
        data = allocation_data_service.load_project_data(
            project_id
        )

        if data["projects_df"].empty:
            raise HTTPException(
                status_code=404,
                detail="Project not found"
            )

        project = data["projects_df"].iloc[0]

        start_date = date.fromisoformat(
            str(project["startDate"])[:10]
        )

        end_date = date.fromisoformat(
            str(project["endDate"])[:10]
        )

        # 2. Run eligibility engine
        eligibility_df = eligibility_engine.evaluate_candidates(
            employees_df=data["employees_df"],
            employee_skills_df=data["employee_skills_df"],
            project_requirements_df=data["project_requirements_df"],
            availability_df=data["availability_df"],
            existing_allocations_df=data["existing_allocations_df"],
            start_date=start_date,
            end_date=end_date,
            requested_hours=requested_hours
        )

        # 3. Run skill matching engine
        skill_df = skill_matching_engine.match_all_employees(
            employees_df=data["employees_df"],
            employee_skills_df=data["employee_skills_df"],
            project_requirements_df=data["project_requirements_df"]
        )

        skill_results = skill_df.to_dict(
            orient="records"
        )

        # 4. Run weighted scoring only for eligible employees
        rankings = scoring_engine.rank_candidates(
            eligibility_df=eligibility_df,
            skill_results=skill_results,
            employees_df=data["employees_df"],
            availability_df=data["availability_df"],
            allocations_df=data["existing_allocations_df"],
            project=project.to_dict(),
            start_date=start_date,
            end_date=end_date,
            requested_hours=requested_hours
        )

        # 5. Include rejected employees separately
        rejected = eligibility_df[
            ~eligibility_df["eligible"]
        ].to_dict(orient="records")

        return {
            "status": "success",
            "projectId": project_id,
            "projectName": project["projectName"],
            "requestedHoursPerWeek": requested_hours,
            "totalEmployees": len(eligibility_df),
            "eligibleCount": len(rankings),
            "rejectedCount": len(rejected),
            "weights": scoring_engine.weights,
            "rankings": rankings,
            "rejectedEmployees": rejected
        }

    except HTTPException:
        raise

    except Exception as error:
        raise HTTPException(
            status_code=502,
            detail=str(error)
        )
