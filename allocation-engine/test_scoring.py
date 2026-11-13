
import pandas as pd

from app.services.scoring_engine import scoring_engine


employees_df = pd.DataFrame([
    {
        "id": 1,
        "employeeCode": "EMP001",
        "name": "Arun Kumar",
        "weeklyCapacityHours": 40
    },
    {
        "id": 2,
        "employeeCode": "EMP002",
        "name": "Priya",
        "weeklyCapacityHours": 40
    }
])

eligibility_df = pd.DataFrame([
    {
        "employeeId": 1,
        "eligible": True
    },
    {
        "employeeId": 2,
        "eligible": False
    }
])

skill_results = [
    {
        "employeeId": 1,
        "skillDetails": [
            {
                "skillId": 1,
                "mandatory": True,
                "matchPercentage": 100
            },
            {
                "skillId": 2,
                "mandatory": True,
                "matchPercentage": 100
            },
            {
                "skillId": 3,
                "mandatory": False,
                "matchPercentage": 50
            }
        ]
    },
    {
        "employeeId": 2,
        "skillDetails": []
    }
]

availability_df = pd.DataFrame([
    {
        "employeeId": 1,
        "startDate": "2026-11-16",
        "endDate": "2027-03-31",
        "availableHoursPerWeek": 30
    }
])

allocations_df = pd.DataFrame(columns=[
    "employeeId",
    "startDate",
    "endDate",
    "allocatedHoursPerWeek",
    "status"
])

project = {
    "id": 1,
    "projectName": "AI-Powered Fraud Detection",
    "priority": 5
}


rankings = scoring_engine.rank_candidates(
    eligibility_df=eligibility_df,
    skill_results=skill_results,
    employees_df=employees_df,
    availability_df=availability_df,
    allocations_df=allocations_df,
    project=project,
    start_date="2026-11-16",
    end_date="2027-03-31",
    requested_hours=20
)


print("\nWEIGHTED SCORING RESULTS")

for candidate in rankings:

    print("\nEmployee:", candidate["employeeName"])
    print("Rank:", candidate["rank"])
    print("Mandatory skill:", candidate["mandatorySkillScore"])
    print("Availability:", candidate["availabilityScore"])
    print("Project relevance:", candidate["projectRelevanceScore"])
    print("Workload balance:", candidate["workloadBalanceScore"])
    print("Optional skill:", candidate["optionalSkillScore"])
    print("Final score:", candidate["finalScore"])
