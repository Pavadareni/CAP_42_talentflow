
import pandas as pd

from app.services.allocation_optimizer import allocation_optimizer


# --------------------------------------
# Eligible candidates and weighted scores
# --------------------------------------

eligible_candidates = [
    {
        "employeeId": 1,
        "employeeCode": "EMP001",
        "employeeName": "Arun",
        "finalScore": 90
    },
    {
        "employeeId": 2,
        "employeeCode": "EMP002",
        "employeeName": "Priya",
        "finalScore": 88
    },
    {
        "employeeId": 3,
        "employeeCode": "EMP003",
        "employeeName": "Rahul",
        "finalScore": 82
    },
    {
        "employeeId": 4,
        "employeeCode": "EMP004",
        "employeeName": "Divya",
        "finalScore": 85
    }
]


# --------------------------------------
# Employee skills
# --------------------------------------

employee_skills_df = pd.DataFrame([

    # Arun
    {"employeeId": 1, "skillId": 1, "proficiencyLevel": "EXPERT"},
    {"employeeId": 1, "skillId": 2, "proficiencyLevel": "BEGINNER"},
    {"employeeId": 1, "skillId": 3, "proficiencyLevel": "ADVANCED"},

    # Priya
    {"employeeId": 2, "skillId": 1, "proficiencyLevel": "ADVANCED"},
    {"employeeId": 2, "skillId": 2, "proficiencyLevel": "EXPERT"},
    {"employeeId": 2, "skillId": 3, "proficiencyLevel": "ADVANCED"},

    # Rahul
    {"employeeId": 3, "skillId": 1, "proficiencyLevel": "INTERMEDIATE"},
    {"employeeId": 3, "skillId": 2, "proficiencyLevel": "ADVANCED"},
    {"employeeId": 3, "skillId": 3, "proficiencyLevel": "BEGINNER"},

    # Divya
    {"employeeId": 4, "skillId": 1, "proficiencyLevel": "BEGINNER"},
    {"employeeId": 4, "skillId": 2, "proficiencyLevel": "EXPERT"},
    {"employeeId": 4, "skillId": 3, "proficiencyLevel": "EXPERT"}

])


# --------------------------------------
# Project requirements
# --------------------------------------

project_requirements_df = pd.DataFrame([

    {
        "skillId": 1,
        "skillName": "Java",
        "requiredLevel": "ADVANCED",
        "requiredHeadcount": 2,
        "mandatory": True
    },

    {
        "skillId": 2,
        "skillName": "Python",
        "requiredLevel": "INTERMEDIATE",
        "requiredHeadcount": 1,
        "mandatory": True
    },

    {
        "skillId": 3,
        "skillName": "SQL",
        "requiredLevel": "ADVANCED",
        "requiredHeadcount": 1,
        "mandatory": True
    }

])


employees_df = pd.DataFrame([
    {"id": 1, "active": True, "weeklyCapacityHours": 40},
    {"id": 2, "active": True, "weeklyCapacityHours": 40},
    {"id": 3, "active": True, "weeklyCapacityHours": 40},
    {"id": 4, "active": True, "weeklyCapacityHours": 40}
])

availability_df = pd.DataFrame([
    {
        "employeeId": 1,
        "startDate": "2026-11-16",
        "endDate": "2027-03-31",
        "availableHoursPerWeek": 30
    },
    {
        "employeeId": 2,
        "startDate": "2026-11-16",
        "endDate": "2027-03-31",
        "availableHoursPerWeek": 35
    },
    {
        "employeeId": 3,
        "startDate": "2026-11-16",
        "endDate": "2027-03-31",
        "availableHoursPerWeek": 30
    },
    {
        "employeeId": 4,
        "startDate": "2026-11-16",
        "endDate": "2027-03-31",
        "availableHoursPerWeek": 30
    }
])

allocations_df = pd.DataFrame([
    {
        "employeeId": 2,
        "startDate": "2026-11-16",
        "endDate": "2027-03-31",
        "allocatedHoursPerWeek": 20,
        "status": "ACTIVE"
    }
])




# --------------------------------------
# Execute optimization
# --------------------------------------

result = allocation_optimizer.optimize(
    eligible_candidates=eligible_candidates,
    employee_skills_df=employee_skills_df,
    project_requirements_df=project_requirements_df,
    employees_df=employees_df,
    availability_df=availability_df,
    allocations_df=allocations_df,
    start_date="2026-11-16",
    end_date="2027-03-31",
    requested_hours=20
)



# --------------------------------------
# Display results
# --------------------------------------

print("\nALLOCATION OPTIMIZATION RESULT")

print("Status:", result["status"])
print("Message:", result["message"])

if result["status"] == "OPTIMAL":

    print("\nSELECTED PROJECT TEAM")

    for employee in result["selectedEmployees"]:

        print(
            employee["employeeCode"],
            employee["employeeName"],
            "Score:", employee["finalScore"]
        )

    print("\nTEAM SUMMARY")
    print("Team size:", result["teamSize"])
    print("Total team score:", result["totalTeamScore"])

    print("\nSKILL COVERAGE")

    for skill in result["skillCoverage"]:

        print(
            skill["skillName"],
            "Required:", skill["requiredHeadcount"],
            "Selected:", skill["selectedQualifiedCount"],
            "Covered:", skill["covered"]
        )
else:

    print("\nUNFILLED REQUIREMENTS")

    for requirement in result["unfilledRequirements"]:
        print(requirement)
