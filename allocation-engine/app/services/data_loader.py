import pandas as pd

from app.services.spring_api_client import spring_client

class AllocationDataLoader:

    def __init__(self):
        self.client = spring_client

    def load_employees(self):
        return self.client.get_employees()

    def load_projects(self, project_id):
        return self.client.get_project(project_id)

    def load_project_skills(self, project_id):
        return self.client.get_project_skills(project_id)

    def load_employee_skills(self, employee_id):
        return self.client.get_employee_skills(employee_id)

    def load_availability(self, employee_id):
        return self.client.get_employee_availability(employee_id)

    def load_allocations(self, employee_id):
        return self.client.get_employee_allocations(employee_id)


data_loader = AllocationDataLoader()
