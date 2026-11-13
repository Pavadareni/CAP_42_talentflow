
import pandas as pd

from app.services.eligibility_engine import eligibility_engine

employees = pd.DataFrame([
    {
        "id": 1,
        "employeeCode": "EMP001",
        "name": "Arun Kumar",
        "active": True,
        "weeklyCapacityHours": 40
    },
    {
        "id": 2,
        "employeeCode": "EMP002",
        "name": "Priya",
        "active": True,
        "weeklyCapacityHours": 40
    },
    {
        "id": 3,
        "employeeCode": "EMP003",
        "name": "Rahul",
        "active": False,
        "weeklyCapacityHours": 40
    }
])

employee_skills = pd.DataFrame([
    {
        "employeeId": 1,
        "skillId": 1,
        "proficiencyLevel": "EXPERT"
    },
    {
        "employeeId": 2,
        "skillId": 1,
        "proficiencyLevel": "INTERMEDIATE"
    }
])

project_requirements = pd.DataFrame([
    {
        "skillId": 1,
        "requiredLevel": "ADVANCED",
        "mandatory": True
    }
])

availability = pd.DataFrame([
    {
        "employeeId": 1,
        "startDate": "2026-11-16",
        "endDate": "2026-11-22",
        "availableHoursPerWeek": 30,
        "isDeleted": False
    },
    {
        "employeeId": 2,
        "startDate": "2026-11-16",
        "endDate": "2026-11-22",
        "availableHoursPerWeek": 30,
        "isDeleted": False
    }
])

allocations = pd.DataFrame(columns=[
    "employeeId",
    "startDate",
    "endDate",
    "allocatedHoursPerWeek",
    "status",
    "isDeleted"
])

results = eligibility_engine.evaluate_candidates(
    employees_df=employees,
    employee_skills_df=employee_skills,
    project_requirements_df=project_requirements,
    availability_df=availability,
    existing_allocations_df=allocations,
    start_date="2026-11-16",
    end_date="2026-11-22",
    requested_hours=20
)

print(results.to_string(index=False))