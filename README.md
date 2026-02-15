# TaskFlow

A full-stack email workflow automation platform that lets users create scheduled email recaps with AI-powered summaries.

## Features

- **User Authentication** - Secure JWT-based login and registration
- **Google OAuth Integration** - Connect your Gmail account
- **Scheduled Workflows** - Create cron-based automated tasks
- **Email Recap** - Get summaries of your recent emails sent to your inbox
- **AI Summarization** - OpenAI-powered email summaries (optional)
- **Manual Triggers** - Run workflows on-demand

## Tech Stack

### Backend
- Java 17
- Spring Boot 4.0
- Spring Security with JWT
- Spring Data JPA
- PostgreSQL
- Google Gmail API
- OpenAI API

### Frontend
- React 19
- Vite
- Tailwind CSS

### Infrastructure
- Docker & Docker Compose
- PostgreSQL 15

## Prerequisites

- Docker and Docker Compose
- Google Cloud Console account (for Gmail API)
- OpenAI API key (optional, for AI summaries)

## Setup

### 1. Clone the repository

```bash
git clone https://github.com/luc27c/TaskFlow.git
cd TaskFlow
```

### 2. Configure environment variables

Copy the example environment file and fill in your credentials:

```bash
cp .env.example .env
```

Edit `.env` with your values:

```env
# Database
DB_USERNAME=postgres
DB_PASSWORD=your-password

# JWT Secret (use a long random string)
JWT_SECRET=your-secret-key-at-least-32-characters

# Google OAuth (from Google Cloud Console)
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret
GOOGLE_REDIRECT_URI=http://localhost:8080/api/auth/google/callback

# OpenAI (optional)
OPENAI_API_KEY=sk-your-openai-key
```

### 3. Set up Google OAuth

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project
3. Enable the Gmail API
4. Configure OAuth consent screen
5. Create OAuth 2.0 credentials (Web application)
6. Add `http://localhost:8080/api/auth/google/callback` as an authorized redirect URI
7. Copy the Client ID and Client Secret to your `.env` file

### 4. Run with Docker

```bash
docker-compose up -d
```

This starts:
- PostgreSQL database on port 5432
- Backend API on port 8080
- Frontend on port 5173

### 5. Access the app

Open http://localhost:5173 in your browser.

## Usage

1. **Register** - Create an account
2. **Connect Gmail** - Authorize the app to access your Gmail
3. **Create Workflow** - Set up an email recap workflow
   - Choose trigger type (Manual or Scheduled)
   - For scheduled, set a time
4. **Run** - Execute manually or wait for the schedule

## Project Structure

```
├── backend/
│   └── src/main/java/com/automation/taskplatform/
│       ├── controller/     # REST endpoints
│       ├── service/        # Business logic
│       ├── model/          # JPA entities
│       ├── repository/     # Data access
│       ├── config/         # Security, JWT
│       └── dto/            # Request/Response objects
├── frontend/
│   └── src/
│       ├── pages/          # React components
│       └── services/       # API client
├── docker-compose.yml
└── .env.example
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login |
| GET | `/api/auth/google/authorize` | Get Google OAuth URL |
| GET | `/api/auth/google/callback` | OAuth callback |
| GET | `/api/auth/google/status` | Check Gmail connection |
| GET | `/api/workflows` | List user workflows |
| POST | `/api/workflows` | Create workflow |
| PUT | `/api/workflows/:id` | Update workflow |
| DELETE | `/api/workflows/:id` | Delete workflow |
| POST | `/api/workflows/:id/run` | Run workflow |

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `DB_USERNAME` | Yes | PostgreSQL username |
| `DB_PASSWORD` | Yes | PostgreSQL password |
| `JWT_SECRET` | Yes | Secret for JWT signing |
| `GOOGLE_CLIENT_ID` | Yes | Google OAuth client ID |
| `GOOGLE_CLIENT_SECRET` | Yes | Google OAuth client secret |
| `OPENAI_API_KEY` | No | Enables AI email summaries |

## License

MIT
