
from app.services.data_loader import data_loader
from app.services.data_processor import data_processor


class AllocationDataService:

    def load_project_data(self, project_id):

        # Load project information
        project = data_loader.load_projects(project_id)

        # Load project skill requirements
        project_skills = data_loader.load_project_skills(project_id)

        # Load all employees
        employees = data_loader.load_employees()

        # Convert employee data
        employees_df = data_processor.employees_to_df(employees)

        # Convert project data
        projects_df = data_processor.projects_to_df(project)

        project_requirements_df = (
            data_processor.project_skills_to_df(project_skills)
        )

        # Prepare employee-level datasets
        employee_skills_list = []
        availability_list = []
        allocations_list = []

        for employee in employees:

            employee_id = employee["id"]

            skills = data_loader.load_employee_skills(employee_id)
            availability = data_loader.load_availability(employee_id)
            allocations = data_loader.load_allocations(employee_id)

            employee_skills_list.extend(skills or [])
            availability_list.extend(availability or [])
            allocations_list.extend(allocations or [])

        # Convert to DataFrames
        employee_skills_df = (
            data_processor.employee_skills_to_df(
                employee_skills_list
            )
        )

        availability_df = (
            data_processor.availability_to_df(
                availability_list
            )
        )

        existing_allocations_df = (
            data_processor.allocations_to_df(
                allocations_list
            )
        )

        return {
            "employees_df": employees_df,
            "employee_skills_df": employee_skills_df,
            "projects_df": projects_df,
            "project_requirements_df": project_requirements_df,
            "availability_df": availability_df,
            "existing_allocations_df": existing_allocations_df
        }


allocation_data_service = AllocationDataService()
