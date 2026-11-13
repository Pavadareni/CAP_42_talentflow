# TalentFlow Frontend
React + Vite frontend for the Employee Skills-to-Project Allocation Optimizer.

## Run locally
1. Install Node.js 20 LTS or later.
2. Open this folder in VS Code or a terminal.
3. Run `npm install`.
4. Copy `.env.example` to `.env` (change URLs if needed).
5. Run `npm run dev` and open `http://localhost:5173`.
6. Run `npm run build` to verify the production build.

## Included screens
- Login using `POST /api/auth/login`
- Admin overview and employee directory
- Admin/manager project and allocation views
- Manager proposal approval/rejection
- Employee self-service profile, skills, availability and allocations
- Python optimizer trigger for a selected project

## Integration assumptions to verify
- Login response exposes `accessToken`, `token`, or `jwt`.
- JWT includes a `roles` claim such as `ROLE_ADMIN`, `ROLE_MANAGER`, or `ROLE_EMPLOYEE`, or login response contains `role`.
- Project and allocation list APIs return arrays or objects with a `content` array.
- Employee self-service endpoints are implemented in Spring Boot.
- Python FastAPI runs at port 8001; change `VITE_OPTIMIZER_BASE_URL` if required.
- Spring Boot CORS must allow `http://localhost:5173`.

The browser must never receive `PYTHON_SERVICE_TOKEN`. That credential is only for Python-to-Spring server communication.
