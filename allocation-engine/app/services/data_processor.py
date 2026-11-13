import pandas as pd


class AllocationDataProcessor:

    @staticmethod
    def employees_to_df(data):

        df = pd.DataFrame(data)

        if df.empty:
            return df

        return df

    @staticmethod
    def employee_skills_to_df(data):

        df = pd.DataFrame(data)

        if df.empty:
            return df

        return df

    @staticmethod
    def projects_to_df(data):

        if isinstance(data, dict):
            data = [data]

        df = pd.DataFrame(data)

        return df

    @staticmethod
    def project_skills_to_df(data):

        df = pd.DataFrame(data)

        return df

    @staticmethod
    def availability_to_df(data):

        df = pd.DataFrame(data)

        if df.empty:
            return df

        for column in ["startDate", "endDate"]:
            if column in df.columns:
                df[column] = pd.to_datetime(
                    df[column],
                    errors="coerce"
                )

        return df

    @staticmethod
    def allocations_to_df(data):

        df = pd.DataFrame(data)

        if df.empty:
            return df

        for column in ["startDate", "endDate"]:
            if column in df.columns:
                df[column] = pd.to_datetime(
                    df[column],
                    errors="coerce"
                )

        if "allocatedHoursPerWeek" in df.columns:
            df["allocatedHoursPerWeek"] = pd.to_numeric(
                df["allocatedHoursPerWeek"],
                errors="coerce"
            )

        return df


data_processor = AllocationDataProcessor()
