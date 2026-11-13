
import requests

from app.core.config import settings


class SpringBootAPIClient:

    def __init__(self):
        self.base_url = settings.SPRING_BOOT_BASE_URL.rstrip("/")
        self.timeout = settings.API_TIMEOUT

        token = settings.PYTHON_SERVICE_TOKEN

        if not token:
            raise RuntimeError(
                "PYTHON_SERVICE_TOKEN is not configured"
            )

        self.service_headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "X-SERVICE-TOKEN": token
        }

        self.session = requests.Session()
        self.session.headers.update(self.service_headers)

    def get(self, endpoint: str):
        url = f"{self.base_url}{endpoint}"
        try:
            print(f"[SPRING API] Calling: GET {url}")

            response = self.session.get(
                url,
                timeout=self.timeout
            )

            print(f"[SPRING API] Status: {response.status_code}")

            response.raise_for_status()

            if not response.content:
                return None

            return response.json()

        except requests.exceptions.HTTPError as error:
            raise RuntimeError(
                f"Spring Boot API failed: "
                f"GET {url} | "
                f"Status: {error.response.status_code} | "
                f"Response: {error.response.text}"
            )

    # def get(self, endpoint: str):
    #     url = f"{self.base_url}{endpoint}"

    #     try:
    #         response = self.session.get(
    #             url,
    #             timeout=self.timeout
    #         )
    #         response.raise_for_status()

    #         if not response.content:
    #             return None

    #         return response.json()

    #     except requests.exceptions.ConnectionError:
    #         raise RuntimeError(
    #             "Cannot connect to Spring Boot. "
    #             "Check whether the backend is running."
    #         )

    #     except requests.exceptions.Timeout:
    #         raise RuntimeError(
    #             "Spring Boot API request timed out."
    #         )

    #     except requests.exceptions.HTTPError as error:
    #         raise RuntimeError(
    #             f"Spring Boot API returned an error: "
    #             f"{error.response.status_code} - "
    #             f"{error.response.text}"
    #         )

    def post(self, endpoint: str, payload, headers=None):
        url = f"{self.base_url}{endpoint}"

        request_headers = dict(self.service_headers)

        if headers:
            request_headers.update(headers)

        try:
            response = self.session.post(
                url,
                json=payload,
                timeout=self.timeout,
                headers=request_headers
            )
            response.raise_for_status()

            if not response.content:
                return None

            return response.json()

        except requests.exceptions.ConnectionError:
            raise RuntimeError(
                "Cannot connect to Spring Boot. "
                "Check whether the backend is running."
            )

        except requests.exceptions.Timeout:
            raise RuntimeError(
                "Spring Boot API request timed out."
            )

        except requests.exceptions.HTTPError as error:
            raise RuntimeError(
                f"Spring Boot API returned an error: "
                f"{error.response.status_code} - "
                f"{error.response.text}"
            )

    def get_employees(self):
        return self.get("/api/employees")

    def get_project(self, project_id: int):
        return self.get(f"/api/projects/{project_id}")

    def get_project_skills(self, project_id: int):
        return self.get(f"/api/projects/{project_id}/skills")

    def get_employee_skills(self, employee_id: int):
        return self.get(
            f"/api/employees/{employee_id}/skills"
        )

    def get_employee_availability(self, employee_id: int):
        return self.get(
            f"/api/employees/{employee_id}/availability"
        )

    def get_employee_allocations(self, employee_id: int):
        return self.get(
            f"/api/employees/{employee_id}/allocations"
        )

    def create_allocation(self, allocation_data: dict):
        return self.post(
            "/api/allocations",
            allocation_data
        )

    def create_allocations(self, allocations: list):
        return self.post(
            "/api/allocations/bulk",
            allocations
        )


spring_client = SpringBootAPIClient()
