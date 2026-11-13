from fastapi import APIRouter, HTTPException

from app.services.allocation_data_service import (
    allocation_data_service
)

router = APIRouter(
    prefix="/data",
    tags=["Allocation Data"]
)


@router.get("/project/{project_id}")
def get_project_data(project_id: int):

    try:

        data = allocation_data_service.load_project_data(project_id)

        return {
            "status": "success",
            "message": "Allocation data loaded successfully",

            "row_counts": {
                key: len(value)
                for key, value in data.items()
            },

            "columns": {
                key: list(value.columns)
                for key, value in data.items()
            }
        }

    except Exception as error:

        raise HTTPException(
            status_code=502,
            detail=str(error)
        )
