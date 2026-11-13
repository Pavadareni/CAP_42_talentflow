
import pandas as pd

from app.services.skill_matcher import skill_matching_engine


# Employee data
employees_df = pd.DataFrame([
    {
        "id": 1,
        "employeeCode": "EMP001",
        "name": "Arun Kumar"
    },
    {
        "id": 2,
        "employeeCode": "EMP002",
        "name": "Priya"
    }
])


# Employee skill data
employee_skills_df = pd.DataFrame([
    {
        "employeeId": 1,
        "skillId": 1,
        "skillName": "Python",
        "proficiencyLevel": "EXPERT"
    },
    {
        "employeeId": 1,
        "skillId": 2,
        "skillName": "SQL",
        "proficiencyLevel": "ADVANCED"
    },
    {
        "employeeId": 1,
        "skillId": 3,
        "skillName": "Machine Learning",
        "proficiencyLevel": "INTERMEDIATE"
    },
    {
        "employeeId": 2,
        "skillId": 1,
        "skillName": "Python",
        "proficiencyLevel": "ADVANCED"
    },
    {
        "employeeId": 2,
        "skillId": 2,
        "skillName": "SQL",
        "proficiencyLevel": "EXPERT"
    },
    {
        "employeeId": 2,
        "skillId": 3,
        "skillName": "Machine Learning",
        "proficiencyLevel": "ADVANCED"
    },
    {
        "employeeId": 2,
        "skillId": 4,
        "skillName": "Docker",
        "proficiencyLevel": "INTERMEDIATE"
    }
])


# Project skill requirements
project_requirements_df = pd.DataFrame([
    {
        "skillId": 1,
        "skillName": "Python",
        "requiredLevel": "ADVANCED",
        "mandatory": True
    },
    {
        "skillId": 2,
        "skillName": "SQL",
        "requiredLevel": "ADVANCED",
        "mandatory": True
    },
    {
        "skillId": 3,
        "skillName": "Machine Learning",
        "requiredLevel": "ADVANCED",
        "mandatory": True
    },
    {
        "skillId": 4,
        "skillName": "Docker",
        "requiredLevel": "INTERMEDIATE",
        "mandatory": False
    }
])


# Execute skill matching
results = skill_matching_engine.match_all_employees(
    employees_df,
    employee_skills_df,
    project_requirements_df
)


# Display summary
summary_columns = [
    "employeeId",
    "employeeName",
    "mandatorySkillCoverage",
    "optionalSkillCoverage",
    "proficiencyMatchScore",
    "mandatorySkillsMatched",
    "mandatorySkillsRequired",
    "optionalSkillsMatched",
    "optionalSkillsRequired"
]

print("\nSKILL MATCHING SUMMARY")
print(results[summary_columns].to_string(index=False))


# Display detailed skill gaps
print("\nSKILL GAPS")

for _, employee in results.iterrows():

    print(f"\nEmployee: {employee['employeeName']}")

    for gap in employee["skillGaps"]:
        print(
            f" - {gap['skillName']}: {gap['gap']}"
        )


# Display individual skill matches
print("\nINDIVIDUAL SKILL MATCHES")

for _, employee in results.iterrows():

    print(f"\nEmployee: {employee['employeeName']}")

    for skill in employee["skillDetails"]:
        print(
            f" - {skill['skillName']}: "
            f"{skill['employeeLevel']} / "
            f"{skill['requiredLevel']} | "
            f"{skill['matchPercentage']}% | "
            f"Matched: {skill['matched']}"
        )
