import pandas as pd

from datetime import timedelta


PROFICIENCY_RANK = {
    "BEGINNER": 1,
    "INTERMEDIATE": 2,
    "ADVANCED": 3,
    "EXPERT": 4
}

ELIGIBLE_ALLOCATION_STATUSES = {
    "PROPOSED",
    "APPROVED",
    "ACTIVE"
}


class EligibilityEngine:

    def __init__(self):
        pass

    @staticmethod
    def _normalize(value):
        if pd.isna(value):
            return ""
        return str(value).strip().upper()

    @staticmethod
    def _get_date(value):
        return pd.to_datetime(value, errors="coerce")

    @staticmethod
    def _is_deleted(records):
        if "isDeleted" not in records.columns:
            return records

        deleted = (
            records["isDeleted"]
            .fillna(False)
            .astype(bool)
        )

        return records[~deleted].copy()

    # -----------------------------------------
    # 1. EMPLOYEE STATUS
    # -----------------------------------------

    def check_employee_status(self, employee):

        active = employee.get("active", False)

        if pd.isna(active):
            return False

        return str(active).strip().lower() in {
            "true", "1", "yes"
        }

    # -----------------------------------------
    # 2. MANDATORY SKILL VALIDATION
    # -----------------------------------------

    def check_mandatory_skills(
        self,
        employee_id,
        employee_skills_df,
        project_requirements_df
    ):

        if project_requirements_df.empty:
            return True, []

        if "employeeId" not in employee_skills_df.columns:
            if employee_skills_df.empty:
                employee_skills = employee_skills_df.copy()
            else:
                return False, [
                    "Employee skills data is missing employeeId"
                ]
        else:
            employee_skills = employee_skills_df[
                employee_skills_df["employeeId"] == employee_id
            ].copy()

        requirements = project_requirements_df.copy()

        if "mandatory" in requirements.columns:
            mandatory = requirements[
                requirements["mandatory"].fillna(True).astype(bool)
            ]
        else:
            mandatory = requirements

        failures = []

        for _, requirement in mandatory.iterrows():

            skill_id = requirement.get("skillId")

            required_level = self._normalize(
                requirement.get("requiredLevel")
            )

            required_rank = PROFICIENCY_RANK.get(
                required_level
            )

            if required_rank is None:
                failures.append(
                    f"Invalid required proficiency: {required_level}"
                )
                continue

            matching = employee_skills[
                employee_skills["skillId"] == skill_id
            ]

            if matching.empty:
                failures.append(
                    f"Missing mandatory skill ID {skill_id}"
                )
                continue

            employee_ranks = (
                matching["proficiencyLevel"]
                .apply(self._normalize)
                .map(PROFICIENCY_RANK)
                .dropna()
            )

            if employee_ranks.empty:
                failures.append(
                    f"Skill ID {skill_id} has no valid proficiency"
                )
                continue

            employee_rank = int(employee_ranks.max())

            if employee_rank < required_rank:
                failures.append(
                    f"Skill ID {skill_id}: requires "
                    f"{required_level}"
                )

        return len(failures) == 0, failures

    # -----------------------------------------
    # 3. AVAILABILITY VALIDATION
    # -----------------------------------------

    def check_availability(
        self,
        employee_id,
        availability_df,
        start_date,
        end_date,
        requested_hours
    ):

        if availability_df.empty:
            return False, "No availability records found"

        if "employeeId" not in availability_df.columns:
            return False, "Availability data is missing employeeId"

        records = availability_df[
            availability_df["employeeId"] == employee_id
        ].copy()

        records = self._is_deleted(records)

        if records.empty:
            return False, "No availability records found"

        current = pd.Timestamp(start_date)
        finish = pd.Timestamp(end_date)

        while current <= finish:

            week_end = current + timedelta(
                days=6 - current.weekday()
            )

            segment_end = min(week_end, finish)

            covered = False

            for _, record in records.iterrows():

                record_start = self._get_date(
                    record.get("startDate")
                )

                record_end = self._get_date(
                    record.get("endDate")
                )

                if pd.isna(record_start) or pd.isna(record_end):
                    continue

                available_hours = pd.to_numeric(
                    record.get("availableHoursPerWeek"),
                    errors="coerce"
                )

                if pd.isna(available_hours):
                    continue

                if (
                    record_start <= current
                    and record_end >= segment_end
                    and available_hours >= requested_hours
                ):
                    covered = True
                    break

            if not covered:
                return False, (
                    f"Insufficient availability for "
                    f"{current.date()} to {segment_end.date()}"
                )

            current = segment_end + timedelta(days=1)

        return True, "Availability validated"

    # -----------------------------------------
    # 4. WEEKLY CAPACITY VALIDATION
    # -----------------------------------------

    def check_capacity(
        self,
        employee,
        allocations_df,
        start_date,
        end_date,
        requested_hours
    ):

        capacity = pd.to_numeric(
            employee.get("weeklyCapacityHours"),
            errors="coerce"
        )

        if pd.isna(capacity):
            return False, "Employee weekly capacity is invalid"

        if requested_hours > capacity:
            return False, "Requested hours exceed weekly capacity"

        employee_id = employee["id"]

        if allocations_df.empty:
            return True, "Capacity validated"

        if "employeeId" not in allocations_df.columns:
            return False, "Allocation data is missing employeeId"

        records = allocations_df[
            allocations_df["employeeId"] == employee_id
        ].copy()

        records = self._is_deleted(records)

        if "status" in records.columns:
            records = records[
                records["status"].apply(self._normalize).isin(
                    ELIGIBLE_ALLOCATION_STATUSES
                )
            ]

        current = pd.Timestamp(start_date)
        finish = pd.Timestamp(end_date)

        while current <= finish:

            week_end = current + timedelta(
                days=6 - current.weekday()
            )

            segment_end = min(week_end, finish)

            committed = 0.0

            for _, allocation in records.iterrows():

                allocation_start = self._get_date(
                    allocation.get("startDate")
                )

                allocation_end = self._get_date(
                    allocation.get("endDate")
                )

                if (
                    pd.isna(allocation_start)
                    or pd.isna(allocation_end)
                ):
                    continue

                overlaps = (
                    allocation_start <= segment_end
                    and allocation_end >= current
                )

                if overlaps:

                    hours = pd.to_numeric(
                        allocation.get("allocatedHoursPerWeek"),
                        errors="coerce"
                    )

                    if not pd.isna(hours):
                        committed += float(hours)

            if committed + requested_hours > float(capacity):
                return False, (
                    f"Weekly capacity exceeded for "
                    f"{current.date()} to {segment_end.date()}"
                )

            current = segment_end + timedelta(days=1)

        return True, "Capacity validated"

    # -----------------------------------------
    # 5. COMPLETE CANDIDATE EVALUATION
    # -----------------------------------------

    def evaluate_candidates(
        self,
        employees_df,
        employee_skills_df,
        project_requirements_df,
        availability_df,
        existing_allocations_df,
        start_date,
        end_date,
        requested_hours
    ):

        results = []

        if pd.Timestamp(end_date) < pd.Timestamp(start_date):
            raise ValueError("End date cannot be before start date")

        if requested_hours <= 0:
            raise ValueError("Requested hours must be greater than zero")

        for _, employee in employees_df.iterrows():

            employee_id = employee["id"]

            rejection_reasons = []

            # Check employee status
            if not self.check_employee_status(employee):
                rejection_reasons.append(
                    "Employee is inactive"
                )

            # Check mandatory skills
            skills_ok, skill_errors = self.check_mandatory_skills(
                employee_id,
                employee_skills_df,
                project_requirements_df
            )

            if not skills_ok:
                rejection_reasons.extend(skill_errors)

            # Check availability
            availability_ok, availability_reason = (
                self.check_availability(
                    employee_id,
                    availability_df,
                    start_date,
                    end_date,
                    requested_hours
                )
            )

            if not availability_ok:
                rejection_reasons.append(
                    availability_reason
                )

            # Check capacity
            capacity_ok, capacity_reason = self.check_capacity(
                employee,
                existing_allocations_df,
                start_date,
                end_date,
                requested_hours
            )

            if not capacity_ok:
                rejection_reasons.append(
                    capacity_reason
                )

            results.append({

                "employeeId": employee_id,

                "employeeCode": employee.get(
                    "employeeCode"
                ),

                "employeeName": employee.get(
                    "name"
                ),

                "eligible": len(rejection_reasons) == 0,

                "rejectionReasons": rejection_reasons

            })

        return pd.DataFrame(results)


# -----------------------------------------
# GLOBAL ENGINE INSTANCE
# -----------------------------------------

eligibility_engine = EligibilityEngine()
