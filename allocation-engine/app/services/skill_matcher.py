
import pandas as pd


PROFICIENCY_RANK = {
    "BEGINNER": 1,
    "INTERMEDIATE": 2,
    "ADVANCED": 3,
    "EXPERT": 4
}


class SkillMatchingEngine:

    def __init__(self):
        self.proficiency_rank = PROFICIENCY_RANK

    @staticmethod
    def normalize_level(value):
        if pd.isna(value):
            return None

        return str(value).strip().upper()

    @staticmethod
    def normalize_skill_id(value):
        if pd.isna(value):
            return None

        # Handles integer IDs and numeric IDs returned as strings
        try:
            return int(value)
        except (ValueError, TypeError):
            return str(value).strip()

    def calculate_proficiency_match(
        self,
        employee_level,
        required_level
    ):
        employee_rank = self.proficiency_rank.get(
            self.normalize_level(employee_level)
        )

        required_rank = self.proficiency_rank.get(
            self.normalize_level(required_level)
        )

        if required_rank is None:
            raise ValueError(
                f"Invalid required proficiency level: {required_level}"
            )

        if employee_rank is None:
            return 0.0

        return round(
            min(employee_rank / required_rank, 1.0) * 100,
            2
        )

    def match_employee_skills(
        self,
        employee_id,
        employee_skills_df,
        project_requirements_df
    ):

        employee_skills = employee_skills_df.copy()
        requirements = project_requirements_df.copy()

        if requirements.empty:
            return {
                "employeeId": employee_id,
                "mandatorySkillCoverage": 100.0,
                "optionalSkillCoverage": None,
                "proficiencyMatchScore": 100.0,
                "skillMatchScore": 100.0,
                "mandatorySkillsMatched": 0,
                "mandatorySkillsRequired": 0,
                "optionalSkillsMatched": 0,
                "optionalSkillsRequired": 0,
                "skillGaps": [],
                "skillDetails": []
            }

        if "skillId" not in requirements.columns:
            raise ValueError(
                "Project requirements must contain skillId"
            )

        if "skillId" not in employee_skills.columns:
            raise ValueError(
                "Employee skills must contain skillId"
            )

        employee_skills = employee_skills[
            employee_skills["employeeId"] == employee_id
        ].copy()

        requirements["_skill_key"] = requirements[
            "skillId"
        ].apply(self.normalize_skill_id)

        employee_skills["_skill_key"] = employee_skills[
            "skillId"
        ].apply(self.normalize_skill_id)

        if "mandatory" not in requirements.columns:
            requirements["mandatory"] = True
        else:
            requirements["mandatory"] = (
                requirements["mandatory"]
                .fillna(True)
                .astype(bool)
            )

        details = []
        gaps = []

        mandatory_scores = []
        optional_scores = []

        mandatory_matched = 0
        optional_matched = 0

        for _, requirement in requirements.iterrows():

            skill_id = requirement["skillId"]
            skill_key = requirement["_skill_key"]

            skill_name = requirement.get(
                "skillName",
                f"Skill {skill_id}"
            )

            required_level = self.normalize_level(
                requirement.get("requiredLevel")
            )

            mandatory = bool(requirement["mandatory"])

            if required_level not in self.proficiency_rank:
                raise ValueError(
                    f"Invalid required level '{required_level}' "
                    f"for skill {skill_name}"
                )

            matching = employee_skills[
                employee_skills["_skill_key"] == skill_key
            ]

            if matching.empty:

                employee_level = None
                match_score = 0.0
                matched = False

            else:

                # If duplicate skill records exist, use the highest
                # recorded proficiency.
                employee_levels = matching[
                    "proficiencyLevel"
                ].apply(self.normalize_level)

                employee_ranks = employee_levels.map(
                    self.proficiency_rank
                ).dropna()

                if employee_ranks.empty:
                    employee_level = None
                    match_score = 0.0
                    matched = False

                else:
                    best_rank = int(employee_ranks.max())

                    employee_level = next(
                        level
                        for level, rank in self.proficiency_rank.items()
                        if rank == best_rank
                    )

                    match_score = self.calculate_proficiency_match(
                        employee_level,
                        required_level
                    )

                    matched = best_rank >= self.proficiency_rank[
                        required_level
                    ]

            detail = {
                "skillId": skill_id,
                "skillName": skill_name,
                "mandatory": mandatory,
                "requiredLevel": required_level,
                "employeeLevel": employee_level,
                "matchPercentage": match_score,
                "matched": matched
            }

            details.append(detail)

            if mandatory:
                mandatory_scores.append(match_score)

                if matched:
                    mandatory_matched += 1
                else:
                    gaps.append({
                        "skillId": skill_id,
                        "skillName": skill_name,
                        "requiredLevel": required_level,
                        "employeeLevel": employee_level,
                        "mandatory": True,
                        "gap": (
                            "Skill missing"
                            if employee_level is None
                            else "Proficiency below required level"
                        )
                    })

            else:
                optional_scores.append(match_score)

                if matched:
                    optional_matched += 1
                else:
                    gaps.append({
                        "skillId": skill_id,
                        "skillName": skill_name,
                        "requiredLevel": required_level,
                        "employeeLevel": employee_level,
                        "mandatory": False,
                        "gap": (
                            "Optional skill missing"
                            if employee_level is None
                            else "Optional proficiency below requirement"
                        )
                    })

        mandatory_coverage = (
            round(
                mandatory_matched / len(mandatory_scores) * 100,
                2
            )
            if mandatory_scores else 100.0
        )

        optional_coverage = (
            round(
                optional_matched / len(optional_scores) * 100,
                2
            )
            if optional_scores else None
        )

        all_scores = mandatory_scores + optional_scores

        proficiency_score = (
            round(sum(all_scores) / len(all_scores), 2)
            if all_scores else 100.0
        )

        return {
            "employeeId": employee_id,
            "mandatorySkillCoverage": mandatory_coverage,
            "optionalSkillCoverage": optional_coverage,
            "proficiencyMatchScore": proficiency_score,
            "skillMatchScore": proficiency_score,
            "mandatorySkillsMatched": mandatory_matched,
            "mandatorySkillsRequired": len(mandatory_scores),
            "optionalSkillsMatched": optional_matched,
            "optionalSkillsRequired": len(optional_scores),
            "skillGaps": gaps,
            "skillDetails": details
        }

    def match_all_employees(
        self,
        employees_df,
        employee_skills_df,
        project_requirements_df
    ):

        results = []

        for _, employee in employees_df.iterrows():

            result = self.match_employee_skills(
                employee_id=employee["id"],
                employee_skills_df=employee_skills_df,
                project_requirements_df=project_requirements_df
            )

            result["employeeName"] = employee.get("name")
            result["employeeCode"] = employee.get("employeeCode")

            results.append(result)

        return pd.DataFrame(results)


skill_matching_engine = SkillMatchingEngine()
