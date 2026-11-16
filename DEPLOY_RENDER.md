# SRS Analyzer - Render deployment

## 1. Push the project to GitHub
Push the repository containing `frontend/`, `backend/`, and `render.yaml`.

## 2. Create the Render Blueprint
In Render: New -> Blueprint -> connect the GitHub repository.
Render will create the backend, frontend static site, and PostgreSQL database from `render.yaml`.

## 3. Secret/config values
Set these values when Render asks for them:

### Backend
- `LLM_API_KEY`: your Gemini API key
- `APP_CORS_ORIGIN`: the final frontend URL, for example `https://srs-analyzer-frontend.onrender.com`

### Frontend
- `VITE_API_BASE_URL`: the final backend URL + `/api`, for example `https://srs-analyzer-backend.onrender.com/api`

Database host/name/user/password variables are linked automatically from the Render PostgreSQL database. The Spring Boot app builds the required JDBC URL from those variables.

### Gui email (tuy chon)
Them thu cong trong Render -> backend -> Environment (khong de gia tri rong, neu khong dung thi bo qua):
- `SPRING_MAIL_HOST` (vd `smtp.gmail.com`), `SPRING_MAIL_PORT` (`587`)
- `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` (Gmail: dung App Password)
- `MAIL_FROM` (dia chi nguoi gui; voi Gmail phai trung username)

## 4. After first deployment
If the frontend/backend URLs changed, update:
- frontend `VITE_API_BASE_URL`
- backend `APP_CORS_ORIGIN`
then redeploy the affected service.

## 5. Local development
Frontend:
`npm install`
`npm run dev`

Backend requires:
- Java 17
- Maven
- PostgreSQL
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- optional `LLM_API_KEY` for real Gemini analysis; without it the project can use its demo/mock analyzer if supported by the current code.

## Security
Do not commit API keys or database passwords. Rotate any credentials that were previously stored in source/config files.
