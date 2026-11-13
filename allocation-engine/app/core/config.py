from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):

    SPRING_BOOT_BASE_URL: str = "http://localhost:8080"
    API_TIMEOUT: int = 15

    PYTHON_SERVICE_TOKEN: str = ""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )


settings = Settings()