# AI Finance Controller — Razorpay Buildathon

Razorpay-native AI-assisted financial reconciliation and exception-resolution system.

## Goal
Build one complete finance-operations loop:
Razorpay payment/settlement data → deterministic reconciliation → exception detection → AI investigation → confidence-based resolution → human escalation → audit trail.

## Build philosophy
- Financial truth comes from structured data and deterministic rules.
- AI investigates ambiguous cases; it does not invent financial facts.
- Every automated decision must be explainable, bounded, and auditable.
- Prefer a small, working system over unnecessary infrastructure.
- Optional features are postponed until the core loop is reliable.

## Primary stack
- Backend: Java 21 + Spring Boot 3.x + Maven
- Frontend: React 18 + Vite + Tailwind CSS
- Database: PostgreSQL 17
- AI: LLM API behind a backend abstraction (Phase 5+)
- Integration: Razorpay test-mode APIs (Phase 2+)
- Synthetic data: controlled generator for 50+ and 1,000+ record evaluation (Phase 2+)

## Local Setup

### Prerequisites
- Java 21+
- Maven 3.9+
- Node 18+
- PostgreSQL 14+

### 1. Configure environment
`ash
cp .env.example .env
# Edit .env and fill in DB_URL, DB_USERNAME, DB_PASSWORD
`

### 2. Create database
`sql
CREATE DATABASE aifincontroller;
CREATE USER aifincontroller WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE aifincontroller TO aifincontroller;
`

### 3. Run backend
`ash
# Load .env first (PowerShell):
Get-Content .env | ForEach-Object { if ( -match '^([^#][^=]*)=(.*)') { [System.Environment]::SetEnvironmentVariable([1].Trim(), [2].Trim()) } }

cd backend
mvn spring-boot:run
# Backend starts at http://localhost:8080
`

### 4. Run frontend
`ash
cd frontend
npm install
npm run dev
# Frontend starts at http://localhost:5173
`

### 5. Health check
`ash
curl http://localhost:8080/api/health
# Expected: { status: UP, database: UP, ... }
`

## API Endpoints (Phase 01)
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/health | Service + database health |

## Core phases
1. **Foundation** ← current
2. Razorpay Data Layer
3. Reconciliation Engine
4. Exception Engine
5. AI Investigator
6. Controller / Decision Engine
7. Audit + Dashboard
8. Reliability + Final Demo

See PROJECT_STATUS.md for current progress.
