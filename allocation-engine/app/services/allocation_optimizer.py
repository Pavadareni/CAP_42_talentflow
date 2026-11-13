
import pandas as pd
import pulp

from datetime import timedelta


class AllocationOptimizer:

    PROFICIENCY_RANK = {
        "BEGINNER": 1,
        "INTERMEDIATE": 2,
        "ADVANCED": 3,
        "EXPERT": 4
    }

    CONSUMING_STATUSES = {
        "PROPOSED",
        "APPROVED",
        "ACTIVE"
    }

    def __init__(self):
        self.solver = pulp.COIN_CMD(msg=False)

    # ==================================================
    # 1. HELPER METHODS
    # ==================================================

    @staticmethod
    def normalize(value):

        if pd.isna(value):
            return ""

        return str(value).strip().upper()

    @staticmethod
    def number(value, default=0.0):

        result = pd.to_numeric(value, errors="coerce")

        if pd.isna(result):
            return default

        return float(result)

    @staticmethod
    def to_date(value):

        return pd.to_datetime(value).date()

    def is_mandatory(self, value):

        if pd.isna(value):
            return True

        if isinstance(value, bool):
            return value

        return str(value).strip().lower() not in {
            "false", "0", "no"
        }

    def is_active(self, value):

        return self.normalize(value) not in {
            "FALSE", "0", "NO"
        }

    # ==================================================
    # 2. EMPLOYEE SKILL MATCHING
    # ==================================================

    def get_skill_rank(
        self,
        employee_skills_df,
        employee_id,
        skill_id
    ):

        if employee_skills_df.empty:
            return 0

        records = employee_skills_df[
            (employee_skills_df["employeeId"] == employee_id)
            & (employee_skills_df["skillId"] == skill_id)
        ].copy()

        if "isDeleted" in records.columns:

            deleted = records["isDeleted"].apply(
                lambda value: self.normalize(value)
                in {"TRUE", "1", "YES"}
            )

            records = records[~deleted]

        if records.empty:
            return 0

        ranks = (
            records["proficiencyLevel"]
            .apply(self.normalize)
            .map(self.PROFICIENCY_RANK)
            .dropna()
        )

        if ranks.empty:
            return 0

        return int(ranks.max())

    # ==================================================
    # 3. GET EMPLOYEE CAPACITY
    # ==================================================

    def get_remaining_capacity(
        self,
        employee_id,
        employees_df,
        availability_df,
        allocations_df,
        start_date,
        end_date
    ):

        start_date = self.to_date(start_date)
        end_date = self.to_date(end_date)

        if end_date < start_date:

            raise ValueError(
                "Project end date cannot be before start date"
            )

        # Find employee.

        employee_records = employees_df[
            employees_df["id"] == employee_id
        ]

        if employee_records.empty:

            return {
                "feasible": False,
                "reason": "Employee record not found",
                "minimumRemainingHours": 0,
                "weeklyBreakdown": []
            }

        employee = employee_records.iloc[0]

        # Check employee active status.

        if not self.is_active(employee.get("active", True)):

            return {
                "feasible": False,
                "reason": "Employee is inactive",
                "minimumRemainingHours": 0,
                "weeklyBreakdown": []
            }

        weekly_capacity = self.number(
            employee["weeklyCapacityHours"]
        )

        if weekly_capacity <= 0:

            return {
                "feasible": False,
                "reason": "Employee has no weekly capacity",
                "minimumRemainingHours": 0,
                "weeklyBreakdown": []
            }

        # Get employee availability.

        if (
            availability_df is not None
            and not availability_df.empty
            and "employeeId" in availability_df.columns
        ):

            employee_availability = availability_df[
                availability_df["employeeId"] == employee_id
            ].copy()

        else:

            employee_availability = pd.DataFrame()

        # Get employee allocations.

        if (
            allocations_df is not None
            and not allocations_df.empty
            and "employeeId" in allocations_df.columns
        ):

            employee_allocations = allocations_df[
                allocations_df["employeeId"] == employee_id
            ].copy()

        else:

            employee_allocations = pd.DataFrame()

        # Ignore soft-deleted availability records.

        if (
            not employee_availability.empty
            and "isDeleted" in employee_availability.columns
        ):

            deleted = employee_availability["isDeleted"].apply(
                lambda value: self.normalize(value)
                in {"TRUE", "1", "YES"}
            )

            employee_availability = employee_availability[
                ~deleted
            ]

        # Ignore soft-deleted allocation records.

        if (
            not employee_allocations.empty
            and "isDeleted" in employee_allocations.columns
        ):

            deleted = employee_allocations["isDeleted"].apply(
                lambda value: self.normalize(value)
                in {"TRUE", "1", "YES"}
            )

            employee_allocations = employee_allocations[
                ~deleted
            ]

        # Only active commitments consume capacity.

        if (
            not employee_allocations.empty
            and "status" in employee_allocations.columns
        ):

            employee_allocations = employee_allocations[
                employee_allocations["status"]
                .apply(self.normalize)
                .isin(self.CONSUMING_STATUSES)
            ]

        weekly_breakdown = []

        minimum_remaining = float("inf")

        week_start = start_date

        while week_start <= end_date:

            week_end = min(
                week_start + timedelta(days=6),
                end_date
            )

            # ------------------------------------------
            # FIND AVAILABILITY COVERING THIS WEEK
            # ------------------------------------------

            if not employee_availability.empty:

                availability_records = (
                    employee_availability.copy()
                )

                availability_records["_start"] = (
                    pd.to_datetime(
                        availability_records["startDate"]
                    ).dt.date
                )

                availability_records["_end"] = (
                    pd.to_datetime(
                        availability_records["endDate"]
                    ).dt.date
                )

                availability_records = availability_records[
                    (availability_records["_start"] <= week_start)
                    & (availability_records["_end"] >= week_end)
                ]

            else:

                availability_records = pd.DataFrame()

            if availability_records.empty:

                return {
                    "feasible": False,
                    "reason": (
                        f"No availability record covering "
                        f"{week_start} to {week_end}"
                    ),
                    "minimumRemainingHours": 0,
                    "weeklyBreakdown": weekly_breakdown
                }

            available_hours = self.number(
                availability_records[
                    "availableHoursPerWeek"
                ].max()
            )

            effective_capacity = min(
                weekly_capacity,
                available_hours
            )

            # ------------------------------------------
            # CALCULATE EXISTING ALLOCATIONS
            # ------------------------------------------

            if not employee_allocations.empty:

                allocation_records = (
                    employee_allocations.copy()
                )

                allocation_records["_start"] = (
                    pd.to_datetime(
                        allocation_records["startDate"]
                    ).dt.date
                )

                allocation_records["_end"] = (
                    pd.to_datetime(
                        allocation_records["endDate"]
                    ).dt.date
                )

                allocation_records = allocation_records[
                    (allocation_records["_start"] <= week_end)
                    & (allocation_records["_end"] >= week_start)
                ]

                committed_hours = sum(
                    self.number(value)
                    for value in allocation_records[
                        "allocatedHoursPerWeek"
                    ]
                )

            else:

                committed_hours = 0.0

            remaining_hours = (
                effective_capacity - committed_hours
            )

            minimum_remaining = min(
                minimum_remaining,
                remaining_hours
            )

            weekly_breakdown.append({

                "weekStart": str(week_start),

                "weekEnd": str(week_end),

                "weeklyCapacityHours": weekly_capacity,

                "availableHoursPerWeek": available_hours,

                "effectiveCapacityHours": effective_capacity,

                "existingAllocatedHours": committed_hours,

                "remainingHours": remaining_hours

            })

            week_start += timedelta(days=7)

        return {

            "feasible": minimum_remaining >= 0,

            "reason": (
                "Capacity available"
                if minimum_remaining >= 0
                else "Existing allocations exceed capacity"
            ),

            "minimumRemainingHours": (
                minimum_remaining
                if minimum_remaining != float("inf")
                else 0
            ),

            "weeklyBreakdown": weekly_breakdown

        }

    # ==================================================
    # 4. MAIN OPTIMIZATION METHOD
    # ==================================================

    def optimize(
        self,
        eligible_candidates,
        employee_skills_df,
        project_requirements_df,
        employees_df,
        availability_df,
        allocations_df,
        start_date,
        end_date,
        requested_hours
    ):

        # ------------------------------------------
        # CHECK SOLVER
        # ------------------------------------------

        if not self.solver.available():

            raise RuntimeError(
                "CBC solver is unavailable. "
                "Run: pip install 'pulp[cbc]'"
            )

        # ------------------------------------------
        # VALIDATE DATES AND REQUESTED HOURS
        # ------------------------------------------

        start_date = self.to_date(start_date)
        end_date = self.to_date(end_date)

        if end_date < start_date:

            raise ValueError(
                "Project end date cannot be before start date"
            )

        requested_hours = self.number(requested_hours)

        if requested_hours <= 0:

            raise ValueError(
                "Requested hours must be greater than zero"
            )

        # ------------------------------------------
        # VALIDATE DATAFRAMES
        # ------------------------------------------

        if employees_df is None or employees_df.empty:

            raise ValueError(
                "Employee dataframe cannot be empty"
            )

        if employee_skills_df is None:

            employee_skills_df = pd.DataFrame(
                columns=[
                    "employeeId",
                    "skillId",
                    "proficiencyLevel"
                ]
            )

        if availability_df is None:

            availability_df = pd.DataFrame()

        if allocations_df is None:

            allocations_df = pd.DataFrame()

        if not eligible_candidates:

            return self.empty_result(
                "INSUFFICIENT_CANDIDATES",
                "No eligible employees available"
            )

        if (
            project_requirements_df is None
            or project_requirements_df.empty
        ):

            return self.empty_result(
                "NO_REQUIREMENTS",
                "No project requirements configured"
            )

        # ------------------------------------------
        # VALIDATE EMPLOYEE DATA
        # ------------------------------------------

        required_employee_columns = {
            "id",
            "active",
            "weeklyCapacityHours"
        }

        missing = required_employee_columns - set(
            employees_df.columns
        )

        if missing:

            raise ValueError(
                f"Employee dataframe missing columns: {sorted(missing)}"
            )

        # ------------------------------------------
        # VALIDATE PROJECT REQUIREMENTS
        # ------------------------------------------

        required_requirement_columns = {
            "skillId",
            "requiredLevel",
            "requiredHeadcount"
        }

        missing = required_requirement_columns - set(
            project_requirements_df.columns
        )

        if missing:

            raise ValueError(
                f"Project requirements missing columns: {sorted(missing)}"
            )

        # ------------------------------------------
        # VALIDATE EMPLOYEE SKILLS
        # ------------------------------------------

        required_skill_columns = {
            "employeeId",
            "skillId",
            "proficiencyLevel"
        }

        missing = required_skill_columns - set(
            employee_skills_df.columns
        )

        if missing:

            raise ValueError(
                f"Employee skills missing columns: {sorted(missing)}"
            )

        # ------------------------------------------
        # FILTER MANDATORY REQUIREMENTS
        # ------------------------------------------

        requirements = project_requirements_df.copy()

        if "mandatory" in requirements.columns:

            requirements = requirements[
                requirements["mandatory"].apply(
                    self.is_mandatory
                )
            ].copy()

        if requirements.empty:

            return self.empty_result(
                "NO_MANDATORY_REQUIREMENTS",
                "No mandatory requirements configured"
            )

        # ------------------------------------------
        # PREPARE UNIQUE CANDIDATES
        # ------------------------------------------

        candidate_map = {}

        for candidate in eligible_candidates:

            employee_id = candidate.get("employeeId")

            if employee_id is None:
                continue

            candidate_map[employee_id] = {

                "employeeId": employee_id,

                "employeeCode": candidate.get(
                    "employeeCode"
                ),

                "employeeName": candidate.get(
                    "employeeName"
                ),

                "finalScore": self.number(
                    candidate.get("finalScore")
                )

            }

        candidates = list(candidate_map.values())

        if not candidates:

            return self.empty_result(
                "INSUFFICIENT_CANDIDATES",
                "No valid candidate records"
            )

        # ==========================================
        # STAGE 8A: CAPACITY FILTERING
        # ==========================================

        capacity_rejections = []

        capacity_eligible = []

        for candidate in candidates:

            employee_id = candidate["employeeId"]

            capacity = self.get_remaining_capacity(

                employee_id=employee_id,

                employees_df=employees_df,

                availability_df=availability_df,

                allocations_df=allocations_df,

                start_date=start_date,

                end_date=end_date

            )

            if not capacity["feasible"]:

                capacity_rejections.append({

                    "employeeId": employee_id,

                    "employeeName": candidate["employeeName"],

                    "reason": capacity["reason"]

                })

                continue

            if (
                requested_hours
                > capacity["minimumRemainingHours"]
            ):

                capacity_rejections.append({

                    "employeeId": employee_id,

                    "employeeName": candidate["employeeName"],

                    "reason": (
                        f"Requested {requested_hours} hours, "
                        f"but only "
                        f"{capacity['minimumRemainingHours']} "
                        f"hours remain"
                    )

                })

                continue

            candidate["remainingCapacityHours"] = (
                capacity["minimumRemainingHours"]
            )

            candidate["weeklyCapacityDetails"] = (
                capacity["weeklyBreakdown"]
            )

            capacity_eligible.append(candidate)

        candidates = capacity_eligible

        candidate_map = {

            candidate["employeeId"]: candidate

            for candidate in candidates

        }

        if not candidates:

            return {

                **self.empty_result(
                    "INSUFFICIENT_CAPACITY",
                    "No employees have sufficient capacity"
                ),

                "capacityRejections": capacity_rejections

            }

        employee_ids = [

            candidate["employeeId"]

            for candidate in candidates

        ]

        # ==========================================
        # STAGE 8B: BUILD SKILL COVERAGE MATRIX
        # ==========================================

        coverage = {}

        for _, requirement in requirements.iterrows():

            skill_id = requirement["skillId"]

            if skill_id in coverage:

                raise ValueError(
                    f"Duplicate requirement for skill {skill_id}"
                )

            required_level = self.normalize(
                requirement["requiredLevel"]
            )

            required_rank = self.PROFICIENCY_RANK.get(
                required_level
            )

            if required_rank is None:

                raise ValueError(
                    f"Invalid required level: {required_level}"
                )

            required_headcount = int(
                self.number(
                    requirement["requiredHeadcount"]
                )
            )

            if required_headcount < 1:

                raise ValueError(
                    f"Invalid headcount for skill {skill_id}"
                )

            qualified_employees = []

            for employee_id in employee_ids:

                employee_rank = self.get_skill_rank(

                    employee_skills_df,

                    employee_id,

                    skill_id

                )

                if employee_rank >= required_rank:

                    qualified_employees.append(employee_id)

            coverage[skill_id] = {

                "skillName": requirement.get(
                    "skillName",
                    f"Skill {skill_id}"
                ),

                "requiredLevel": required_level,

                "requiredHeadcount": required_headcount,

                "qualifiedEmployees": qualified_employees

            }

        # ------------------------------------------
        # CHECK SKILL FEASIBILITY
        # ------------------------------------------

        unfilled = []

        for skill_id, details in coverage.items():

            available_count = len(
                details["qualifiedEmployees"]
            )

            required_count = details["requiredHeadcount"]

            if available_count < required_count:

                unfilled.append({

                    "skillId": skill_id,

                    "skillName": details["skillName"],

                    "requiredLevel": details["requiredLevel"],

                    "requiredHeadcount": required_count,

                    "availableQualifiedEmployees": available_count

                })

        if unfilled:

            return {

                **self.empty_result(
                    "INSUFFICIENT_SKILLS",
                    "Not enough qualified employees"
                ),

                "unfilledRequirements": unfilled,

                "capacityRejections": capacity_rejections

            }

        # ==========================================
        # STAGE 8C: FIRST OPTIMIZATION
        # MINIMIZE TEAM SIZE
        # ==========================================

        first_problem = pulp.LpProblem(

            "Minimize_Team_Size",

            pulp.LpMinimize

        )

        x_first = {

            employee_id: first_problem.add_variable(

                f"employee_{employee_id}",

                cat="Binary"

            )

            for employee_id in employee_ids

        }

        first_problem += pulp.lpSum(

            x_first[employee_id]

            for employee_id in employee_ids

        )

        for skill_id, details in coverage.items():

            first_problem += (

                pulp.lpSum(

                    x_first[employee_id]

                    for employee_id in details[
                        "qualifiedEmployees"
                    ]

                )

                >= details["requiredHeadcount"]

            ), f"skill_requirement_{skill_id}"

        first_stats = first_problem.solve(self.solver)

        if (

            not first_stats.has_solution

            or first_stats.status_str != "Optimal"

        ):

            return {

                **self.empty_result(
                    "OPTIMIZATION_FAILED",
                    first_stats.status_str
                ),

                "capacityRejections": capacity_rejections

            }

        minimum_team_size = int(

            round(

                pulp.value(

                    pulp.lpSum(

                        x_first[employee_id]

                        for employee_id in employee_ids

                    )

                )

            )

        )

        # ==========================================
        # STAGE 8D: SECOND OPTIMIZATION
        # MAXIMIZE TEAM SCORE
        # ==========================================

        second_problem = pulp.LpProblem(

            "Maximize_Team_Suitability",

            pulp.LpMaximize

        )

        x_second = {

            employee_id: second_problem.add_variable(

                f"employee_{employee_id}",

                cat="Binary"

            )

            for employee_id in employee_ids

        }

        second_problem += pulp.lpSum(

            candidate_map[employee_id]["finalScore"]

            * x_second[employee_id]

            for employee_id in employee_ids

        )

        second_problem += (

            pulp.lpSum(

                x_second[employee_id]

                for employee_id in employee_ids

            )

            == minimum_team_size

        ), "Minimum_Team_Size"

        for skill_id, details in coverage.items():

            second_problem += (

                pulp.lpSum(

                    x_second[employee_id]

                    for employee_id in details[
                        "qualifiedEmployees"
                    ]

                )

                >= details["requiredHeadcount"]

            ), f"skill_requirement_{skill_id}"

        second_stats = second_problem.solve(self.solver)

        if (

            not second_stats.has_solution

            or second_stats.status_str != "Optimal"

        ):

            return {

                **self.empty_result(
                    "OPTIMIZATION_FAILED",
                    second_stats.status_str
                ),

                "capacityRejections": capacity_rejections

            }

        # ==========================================
        # STAGE 8E: COLLECT SELECTED EMPLOYEES
        # ==========================================

        selected = []

        for employee_id in employee_ids:

            if pulp.value(x_second[employee_id]) > 0.5:

                selected.append(
                    candidate_map[employee_id].copy()
                )

        selected.sort(

            key=lambda item: item["finalScore"],

            reverse=True

        )

        # ==========================================
        # STAGE 8F: SKILL COVERAGE REPORT
        # ==========================================

        skill_coverage = []

        for skill_id, details in coverage.items():

            selected_qualified = [

                employee["employeeId"]

                for employee in selected

                if employee["employeeId"]
                in details["qualifiedEmployees"]

            ]

            selected_count = len(selected_qualified)

            required_count = details["requiredHeadcount"]

            skill_coverage.append({

                "skillId": skill_id,

                "skillName": details["skillName"],

                "requiredLevel": details["requiredLevel"],

                "requiredHeadcount": required_count,

                "selectedQualifiedCount": selected_count,

                "qualifiedEmployeeIds": selected_qualified,

                "covered": selected_count >= required_count

            })

        # ==========================================
        # STAGE 8G: FINAL RESULT
        # ==========================================

        total_team_score = sum(

            employee["finalScore"]

            for employee in selected

        )

        return {

            "status": "OPTIMAL",

            "message": "Optimal project team selected",

            "teamSize": len(selected),

            "totalTeamScore": round(
                total_team_score, 2
            ),

            "requestedHoursPerWeek": requested_hours,

            "projectStartDate": str(start_date),

            "projectEndDate": str(end_date),

            "selectedEmployees": selected,

            "skillCoverage": skill_coverage,

            "capacityRejections": capacity_rejections,

            "unfilledRequirements": []

        }

    # ==================================================
    # 5. EMPTY RESULT HELPER
    # ==================================================

    @staticmethod
    def empty_result(status, message):

        return {

            "status": status,

            "message": message,

            "teamSize": 0,

            "totalTeamScore": 0,

            "selectedEmployees": [],

            "skillCoverage": [],

            "capacityRejections": [],

            "unfilledRequirements": []

        }


# ==================================================
# GLOBAL OPTIMIZER INSTANCE
# ==================================================

allocation_optimizer = AllocationOptimizer()
