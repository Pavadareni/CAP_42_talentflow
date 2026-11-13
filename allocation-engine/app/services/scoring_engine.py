import pandas as pd


class WeightedScoringEngine:

    WEIGHTS = {
        "mandatory_skill": 0.40,
        "availability": 0.20,
        "project_relevance": 0.15,
        "workload_balance": 0.15,
        "optional_skill": 0.10
    }

    def __init__(self):
        self.weights = self.WEIGHTS.copy()

    @staticmethod
    def _number(value, default=0.0):
        result = pd.to_numeric(value, errors="coerce")

        if pd.isna(result):
            return default

        return float(result)

    @staticmethod
    def _date(value):
        return pd.to_datetime(value, errors="coerce")

    @staticmethod
    def _normalize(value):
        if pd.isna(value):
            return ""
        return str(value).strip().upper()

    def calculate_mandatory_skill_score(self, skill_result):

        details = skill_result.get("skillDetails", [])

        mandatory_scores = [
            self._number(skill.get("matchPercentage"))
            for skill in details
            if skill.get("mandatory") is True
        ]

        if not mandatory_scores:
            return 100.0

        return round(
            sum(mandatory_scores) / len(mandatory_scores),
            2
        )

    def calculate_optional_skill_score(self, skill_result):

        details = skill_result.get("skillDetails", [])

        optional_scores = [
            self._number(skill.get("matchPercentage"))
            for skill in details
            if skill.get("mandatory") is False
        ]

        if not optional_scores:
            return 100.0

        return round(
            sum(optional_scores) / len(optional_scores),
            2
        )

    def calculate_availability_score(
        self,
        employee,
        availability_df,
        start_date,
        end_date
    ):

        employee_id = employee["id"]

        records = availability_df.copy()

        if records.empty or "employeeId" not in records.columns:
            return 0.0

        records = records[
            records["employeeId"] == employee_id
        ]

        if "isDeleted" in records.columns:
            records = records[
                ~records["isDeleted"].fillna(False).astype(bool)
            ]

        capacity = self._number(
            employee.get("weeklyCapacityHours")
        )

        if capacity <= 0:
            return 0.0

        current = pd.Timestamp(start_date)
        finish = pd.Timestamp(end_date)

        weekly_scores = []

        while current <= finish:

            week_end = current + pd.Timedelta(
                days=6-current.weekday()
            )

            segment_end = min(week_end, finish)

            available_hours = 0.0

            for _, record in records.iterrows():

                record_start = self._date(
                    record.get("startDate")
                )

                record_end = self._date(
                    record.get("endDate")
                )

                if pd.isna(record_start) or pd.isna(record_end):
                    continue

                if (
                    record_start <= current
                    and record_end >= segment_end
                ):
                    available_hours = max(
                        available_hours,
                        self._number(
                            record.get("availableHoursPerWeek")
                        )
                    )

            score = min(
                (available_hours / capacity) * 100,
                100.0
            )

            weekly_scores.append(score)

            current = segment_end + pd.Timedelta(days=1)

        if not weekly_scores:
            return 0.0

        return round(
            sum(weekly_scores) / len(weekly_scores),
            2
        )

    def calculate_workload_balance_score(
        self,
        employee,
        allocations_df,
        start_date,
        end_date,
        requested_hours
    ):

        capacity = self._number(
            employee.get("weeklyCapacityHours")
        )

        if capacity <= 0:
            return 0.0

        employee_id = employee["id"]

        if allocations_df.empty:
            committed_hours = 0.0

        else:

            if "employeeId" not in allocations_df.columns:
                return 0.0

            records = allocations_df[
                allocations_df["employeeId"] == employee_id
            ].copy()

            if "isDeleted" in records.columns:
                records = records[
                    ~records["isDeleted"].fillna(False).astype(bool)
                ]

            active_statuses = {
                "PROPOSED",
                "APPROVED",
                "ACTIVE"
            }

            if "status" in records.columns:
                records = records[
                    records["status"].apply(
                        self._normalize
                    ).isin(active_statuses)
                ]

            current = pd.Timestamp(start_date)
            finish = pd.Timestamp(end_date)

            weekly_remaining_scores = []

            while current <= finish:

                week_end = current + pd.Timedelta(
                    days=6-current.weekday()
                )

                segment_end = min(week_end, finish)

                committed = 0.0

                for _, allocation in records.iterrows():

                    allocation_start = self._date(
                        allocation.get("startDate")
                    )

                    allocation_end = self._date(
                        allocation.get("endDate")
                    )

                    if (
                        pd.isna(allocation_start)
                        or pd.isna(allocation_end)
                    ):
                        continue

                    if (
                        allocation_start <= segment_end
                        and allocation_end >= current
                    ):
                        committed += self._number(
                            allocation.get(
                                "allocatedHoursPerWeek"
                            )
                        )

                remaining = max(
                    capacity - committed - requested_hours,
                    0.0
                )

                weekly_remaining_scores.append(
                    (remaining / capacity) * 100
                )

                current = segment_end + pd.Timedelta(days=1)

            if not weekly_remaining_scores:
                return 0.0

            return round(
                sum(weekly_remaining_scores)
                / len(weekly_remaining_scores),
                2
            )

        remaining = max(
            capacity - committed_hours - requested_hours,
            0.0
        )

        return round(
            (remaining / capacity) * 100,
            2
        )

    def calculate_project_relevance_score(
        self,
        employee,
        project,
        relevance_score=50
    ):
        # Temporary neutral baseline.
        # Replace this with employee project-history/domain
        # matching when that data is available.

        return max(
            0.0,
            min(self._number(relevance_score, 50.0), 100.0)
        )

    def calculate_final_score(
        self,
        mandatory_skill_score,
        availability_score,
        project_relevance_score,
        workload_balance_score,
        optional_skill_score
    ):

        final_score = (
            mandatory_skill_score
            * self.weights["mandatory_skill"]

            + availability_score
            * self.weights["availability"]

            + project_relevance_score
            * self.weights["project_relevance"]

            + workload_balance_score
            * self.weights["workload_balance"]

            + optional_skill_score
            * self.weights["optional_skill"]
        )

        return round(final_score, 2)

    def rank_candidates(
        self,
        eligibility_df,
        skill_results,
        employees_df,
        availability_df,
        allocations_df,
        project,
        start_date,
        end_date,
        requested_hours
    ):

        rankings = []

        skill_lookup = {
            result["employeeId"]: result
            for result in skill_results
        }

        employee_lookup = {
            row["id"]: row
            for _, row in employees_df.iterrows()
        }

        for _, eligibility in eligibility_df.iterrows():

            employee_id = eligibility["employeeId"]

            # Only eligible employees are scored.
            if not eligibility["eligible"]:
                continue

            employee = employee_lookup.get(employee_id)

            if employee is None:
                continue

            skill_result = skill_lookup.get(
                employee_id,
                {"skillDetails": []}
            )

            mandatory_score = (
                self.calculate_mandatory_skill_score(
                    skill_result
                )
            )

            optional_score = (
                self.calculate_optional_skill_score(
                    skill_result
                )
            )

            availability_score = (
                self.calculate_availability_score(
                    employee,
                    availability_df,
                    start_date,
                    end_date
                )
            )

            workload_score = (
                self.calculate_workload_balance_score(
                    employee,
                    allocations_df,
                    start_date,
                    end_date,
                    requested_hours
                )
            )

            relevance_score = (
                self.calculate_project_relevance_score(
                    employee,
                    project
                )
            )

            final_score = self.calculate_final_score(
                mandatory_score,
                availability_score,
                relevance_score,
                workload_score,
                optional_score
            )

            rankings.append({

                "employeeId": employee_id,

                "employeeCode": employee.get(
                    "employeeCode"
                ),

                "employeeName": employee.get(
                    "name"
                ),

                "mandatorySkillScore": mandatory_score,

                "availabilityScore": availability_score,

                "projectRelevanceScore": relevance_score,

                "workloadBalanceScore": workload_score,

                "optionalSkillScore": optional_score,

                "finalScore": final_score

            })

        rankings.sort(
            key=lambda item: item["finalScore"],
            reverse=True
        )

        for rank, candidate in enumerate(rankings, start=1):
            candidate["rank"] = rank

        return rankings


scoring_engine = WeightedScoringEngine()
