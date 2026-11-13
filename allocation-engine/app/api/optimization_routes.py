
from datetime import date
import re

from fastapi import APIRouter, HTTPException, Query

from app.services.allocation_data_service import allocation_data_service
from app.services.allocation_optimizer import allocation_optimizer
from app.services.eligibility_engine import eligibility_engine
from app.services.skill_matcher import skill_matching_engine
from app.services.scoring_engine import scoring_engine
from app.services.spring_api_client import spring_client


router = APIRouter(
    prefix="/optimization",
    tags=["Allocation Optimization"]
)


# =========================================================
# HELPER FUNCTIONS
# =========================================================

def is_active(value):
    """Safely convert employee/requirement active values to bool."""

    if isinstance(value, str):
        return value.strip().lower() in ("true", "1", "yes")

    return bool(value)


def get_rejection_reasons(employee):
    """Read rejection reasons from either supported response field."""

    reasons = employee.get("rejectionReasons", [])

    if isinstance(reasons, str):
        reasons = [reasons]

    if not reasons and employee.get("reason"):
        reasons = [employee["reason"]]

    return [str(reason) for reason in reasons]


def classify_optimization_failure(
    employees_df,
    project_requirements_df,
    rejected_employees,
    eligible_candidate_count=0
):
    """
    Classify why a complete allocation team could not be formed.

    Priority:
    1. INSUFFICIENT_SKILLS
    2. INSUFFICIENT_CAPACITY
    3. INSUFFICIENT_CANDIDATES
    """

    # -----------------------------------------------------
    # 1. Check active employees
    # -----------------------------------------------------

    if employees_df.empty:
        return (
            "INSUFFICIENT_CANDIDATES",
            "No employees are available."
        ), []

    active_employees = employees_df[
        employees_df["active"].apply(is_active)
    ]

    if active_employees.empty:
        return (
            "INSUFFICIENT_CANDIDATES",
            "No active employees are available."
        ), []

    active_employee_ids = set(
        active_employees["id"].astype(int).tolist()
    )

    # -----------------------------------------------------
    # 2. Collect skill and capacity rejections
    # -----------------------------------------------------

    skill_rejections = {}
    capacity_rejected_ids = set()
    inactive_rejected_ids = set()

    for employee in rejected_employees:

        employee_id = employee.get("employeeId")

        if employee_id is None:
            continue

        employee_id = int(employee_id)

        reasons = get_rejection_reasons(employee)

        for reason in reasons:

            reason_lower = reason.lower()

            # Detect skill rejection.
            skill_match = re.search(
                r"skill\s*id\s*(\d+)",
                reason,
                re.IGNORECASE
            )

            if skill_match:
                skill_id = int(skill_match.group(1))

                skill_rejections.setdefault(
                    skill_id, set()
                ).add(employee_id)

            # Detect capacity rejection.
            if any(word in reason_lower for word in (
                "capacity",
                "hours remain",
                "availability",
                "weekly capacity exceeded",
                "remaining capacity",
                "insufficient hours"
            )):
                capacity_rejected_ids.add(employee_id)

            # Detect inactive employee.
            if "inactive" in reason_lower:
                inactive_rejected_ids.add(employee_id)

    # -----------------------------------------------------
    # 3. Evaluate mandatory skill requirements
    # -----------------------------------------------------

    unfilled_requirements = []
    mandatory_skill_shortage = False

    mandatory_requirements = project_requirements_df[
        project_requirements_df["mandatory"].apply(is_active)
    ]

    for _, requirement in mandatory_requirements.iterrows():

        skill_id = int(requirement["skillId"])

        required_headcount = int(
            requirement["requiredHeadcount"]
        )

        rejected_for_skill = skill_rejections.get(
            skill_id, set()
        )

        # Active employees who have not been rejected
        # for this mandatory skill.
        qualified_employee_ids = (
            active_employee_ids - rejected_for_skill
        )

        qualified_count = len(qualified_employee_ids)

        if qualified_count < required_headcount:

            mandatory_skill_shortage = True

            unfilled_requirements.append({
                "skillId": skill_id,
                "skillName": requirement.get("skillName"),
                "requiredLevel": requirement.get("requiredLevel"),
                "requiredHeadcount": required_headcount,
                "availableQualifiedEmployees": qualified_count
            })

    # -----------------------------------------------------
    # 4. Classify skill shortage
    # -----------------------------------------------------

    if mandatory_skill_shortage:

        return (
            "INSUFFICIENT_SKILLS",
            "Not enough qualified employees to satisfy mandatory project skill requirements."
        ), unfilled_requirements

    # -----------------------------------------------------
    # 5. Classify capacity shortage
    # -----------------------------------------------------

    if capacity_rejected_ids:

        return (
            "INSUFFICIENT_CAPACITY",
            "Employees meet the mandatory skill requirements, but none have sufficient remaining capacity."
        ), unfilled_requirements

    # -----------------------------------------------------
    # 6. Default classification
    # -----------------------------------------------------

    return (
        "INSUFFICIENT_CANDIDATES",
        "No eligible employees are available for this project."
    ), unfilled_requirements


def build_failure_response(
    status,
    message,
    project_id,
    project_name,
    requested_hours,
    eligible_candidate_count=0,
    capacity_rejections=None,
    unfilled_requirements=None,
    rankings=None
):
    """Build a consistent response for unsuccessful optimization."""

    return {
        "status": status,
        "message": message,
        "projectId": project_id,
        "projectName": project_name,
        "requestedHoursPerWeek": requested_hours,
        "teamSize": 0,
        "totalTeamScore": 0,
        "selectedEmployees": [],
        "eligibleCandidateCount": eligible_candidate_count,
        "capacityRejections": capacity_rejections or [],
        "unfilledRequirements": unfilled_requirements or [],
        "rankings": rankings or [],
        "persistenceStatus": "NOT_SAVED",
        "savedAllocations": [],
        "savedAllocationCount": 0
    }


# =========================================================
# OPTIMIZATION ENDPOINT
# =========================================================

@router.post("/project/{project_id}")
def optimize_project(
    project_id: int,
    requested_hours: float = Query(default=20, gt=0)
):

    try:

        # =================================================
        # 1. LOAD PROJECT DATA
        # =================================================

        data = allocation_data_service.load_project_data(
            project_id
        )

        employees_df = data["employees_df"]
        employee_skills_df = data["employee_skills_df"]
        projects_df = data["projects_df"]
        project_requirements_df = data["project_requirements_df"]
        availability_df = data["availability_df"]
        existing_allocations_df = data["existing_allocations_df"]

        if projects_df.empty:

            raise HTTPException(
                status_code=404,
                detail=f"Project {project_id} not found"
            )

        project = projects_df.iloc[0]

        # =================================================
        # 2. CHECK PROJECT REQUIREMENTS
        # =================================================

        if project_requirements_df.empty:

            return build_failure_response(
                status="NO_REQUIREMENTS",
                message="No skill requirements configured for this project.",
                project_id=project_id,
                project_name=project.get("projectName"),
                requested_hours=requested_hours
            )

        # =================================================
        # 3. PROJECT DATE VALIDATION
        # =================================================

        start_date = date.fromisoformat(
            str(project["startDate"])[:10]
        )

        end_date = date.fromisoformat(
            str(project["endDate"])[:10]
        )

        if end_date < start_date:

            raise HTTPException(
                status_code=400,
                detail="Project end date cannot be before start date"
            )

        # =================================================
        # 4. RUN ELIGIBILITY ENGINE
        # =================================================

        eligibility_df = eligibility_engine.evaluate_candidates(
            employees_df=employees_df,
            employee_skills_df=employee_skills_df,
            project_requirements_df=project_requirements_df,
            availability_df=availability_df,
            existing_allocations_df=existing_allocations_df,
            start_date=start_date,
            end_date=end_date,
            requested_hours=requested_hours
        )

        if eligibility_df is None or eligibility_df.empty:

            return build_failure_response(
                status="INSUFFICIENT_CANDIDATES",
                message="No employees were evaluated for this project.",
                project_id=project_id,
                project_name=project.get("projectName"),
                requested_hours=requested_hours
            )

        # =================================================
        # 5. CAPTURE REJECTED EMPLOYEES
        # =================================================

        rejected_employees = eligibility_df[
            eligibility_df["eligible"] == False
        ].to_dict(orient="records")

        capacity_rejections = []

        for employee in rejected_employees:

            reasons = get_rejection_reasons(employee)

            capacity_rejections.append({
                "employeeId": employee.get("employeeId"),
                "employeeCode": employee.get("employeeCode"),
                "employeeName": employee.get("employeeName"),
                "rejectionReasons": reasons
            })

        # =================================================
        # 6. SKILL MATCHING
        # =================================================

        skill_df = skill_matching_engine.match_all_employees(
            employees_df=employees_df,
            employee_skills_df=employee_skills_df,
            project_requirements_df=project_requirements_df
        )

        skill_results = skill_df.to_dict(
            orient="records"
        )

        # =================================================
        # 7. WEIGHTED SCORING
        # =================================================

        eligible_df = eligibility_df[
            eligibility_df["eligible"] == True
        ]

        rankings = scoring_engine.rank_candidates(
            eligibility_df=eligible_df,
            skill_results=skill_results,
            employees_df=employees_df,
            availability_df=availability_df,
            allocations_df=existing_allocations_df,
            project=project.to_dict(),
            start_date=start_date,
            end_date=end_date,
            requested_hours=requested_hours
        )

        eligible_candidates = [
            candidate
            for candidate in rankings
            if candidate.get("employeeId") is not None
        ]

        # =================================================
        # 8. HANDLE ZERO ELIGIBLE CANDIDATES
        # =================================================

        if not eligible_candidates:

            (failure_status, failure_message), unfilled_requirements = (
                classify_optimization_failure(
                    employees_df=employees_df,
                    project_requirements_df=project_requirements_df,
                    rejected_employees=rejected_employees,
                    eligible_candidate_count=0
                )
            )

            return build_failure_response(
                status=failure_status,
                message=failure_message,
                project_id=project_id,
                project_name=project.get("projectName"),
                requested_hours=requested_hours,
                eligible_candidate_count=0,
                capacity_rejections=capacity_rejections,
                unfilled_requirements=unfilled_requirements,
                rankings=[]
            )

        # =================================================
        # 9. RUN PU LP OPTIMIZER
        # =================================================

        result = allocation_optimizer.optimize(
            eligible_candidates=eligible_candidates,
            employee_skills_df=employee_skills_df,
            project_requirements_df=project_requirements_df,
            employees_df=employees_df,
            availability_df=availability_df,
            allocations_df=existing_allocations_df,
            start_date=start_date,
            end_date=end_date,
            requested_hours=requested_hours
        )

        # =================================================
        # 10. ADD PROJECT AND RANKING DETAILS
        # =================================================

        result["projectId"] = project_id
        result["projectName"] = project.get("projectName")
        result["requestedHoursPerWeek"] = requested_hours
        result["eligibleCandidateCount"] = len(eligible_candidates)
        result["rankings"] = rankings

        optimizer_rejections = result.get(
            "capacityRejections", []
        )

        # Merge rejection details without discarding
        # the original eligibility rejection records.
        result["capacityRejections"] = (
            capacity_rejections + optimizer_rejections
        )

        # =================================================
        # 11. HANDLE OPTIMIZER FAILURE
        # =================================================

        selected_employees = result.get(
            "selectedEmployees", []
        )

        if (
            result.get("status") != "OPTIMAL"
            or not selected_employees
        ):

            # Include optimizer-level capacity rejections
            # in classification as well.
            all_rejections = (
                rejected_employees + optimizer_rejections
            )

            (failure_status, failure_message), unfilled_requirements = (
                classify_optimization_failure(
                    employees_df=employees_df,
                    project_requirements_df=project_requirements_df,
                    rejected_employees=all_rejections,
                    eligible_candidate_count=len(eligible_candidates)
                )
            )

            result["status"] = failure_status
            result["message"] = failure_message

            result["unfilledRequirements"] = (
                unfilled_requirements
                or result.get("unfilledRequirements", [])
            )

            result["persistenceStatus"] = "NOT_SAVED"
            result["savedAllocations"] = []
            result["savedAllocationCount"] = 0

            return result

        # =================================================
        # 12. PREPARE ALLOCATION PAYLOAD
        # =================================================

        allocation_payload = []

        for employee in selected_employees:

            employee_id = employee.get("employeeId")

            if employee_id is None:

                raise HTTPException(
                    status_code=500,
                    detail="Selected employee is missing employeeId"
                )

            allocation_payload.append({
                "employeeId": int(employee_id),
                "projectId": int(project_id),
                "startDate": start_date.isoformat(),
                "endDate": end_date.isoformat(),
                "allocatedHoursPerWeek": requested_hours
            })

        # =================================================
        # 13. SAVE ALLOCATIONS THROUGH SPRING BOOT
        # =================================================

        saved_allocations = spring_client.create_allocations(
            allocation_payload
        )

        result["persistenceStatus"] = "SAVED_AS_PROPOSED"
        result["savedAllocations"] = saved_allocations
        result["savedAllocationCount"] = len(
            saved_allocations or []
        )

        return result

    # =====================================================
    # ERROR HANDLING
    # =====================================================

    except HTTPException:
        raise

    except RuntimeError as error:

        raise HTTPException(
            status_code=502,
            detail=f"Spring Boot integration failed: {str(error)}"
        )

    except Exception as error:

        raise HTTPException(
            status_code=500,
            detail=f"Optimization failed: {str(error)}"
        )
