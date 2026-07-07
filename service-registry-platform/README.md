# System Registry & Security Compliance Platform

Golomt-style system registry assignment implementation.

## Current Scope

Current checkpoint:

- Root project structure
- PostgreSQL via Docker Compose
- Spring Boot `platform-api`
- Flyway database schema
- Actuator health endpoint
- JWT login and current-user API (with login brute-force lockout)
- System registry CRUD, list/search/filter
- Security checklist APIs and score calculation
- Audit log API and action tracking
- React registry portal (`frontend/`, port 5173) with live data
- Standalone banking web app (`frontend-banking/`, port 5174)
- Banking Transfer Service: double-entry ledger, pessimistic locking,
  transfer idempotency (`Idempotency-Key` header)
- Transfer status lifecycle (SUCCESS/FAILED/REVERSED) with persisted
  failure reasons, admin-only transfer reversal
- Account statements (date range, opening/closing balance)
- Ownership model: bank customers linked to platform users via username;
  non-admin users only see and use their own accounts (ADMIN = teller)
- Configurable transfer limits (per-transfer max, daily outgoing total)
- Customer and account management APIs (open/block/unblock/close)
- Banking audit log (`bank_audit_logs`) with actor snapshots
- Machine-readable error codes (`code` field in error responses)
- Banking service requires the platform JWT (shared JWT_SECRET, resource server)
- Both services verified on real PostgreSQL (migrations, health, e2e transfer demo)
- GitHub Actions CI workflow (activates after remote push)
- No password/secret fallbacks: services fail fast when env vars are missing

Next tasks:

1. Create GitHub remote, push, enable CI.
2. Frontend tests (Vitest) for both apps.
3. Make monitoring real before adding the deposit service: health checks,
   runtime status, poller (see `docs/registry-monitoring-research.md`).
4. See `docs/development-checklist.md` sections 4.9-4.13 for the full backlog.

Project tracking:

```text
docs/system-concept.md
docs/requirements-analysis.md
docs/registry-monitoring-research.md
docs/development-checklist.md
docs/banking-api-contract.md
```

## Demo Logins

| Username | Password | Role |
| --- | --- | --- |
| `admin` | `admin123` | Portal admin / bank teller (ADMIN) |
| `batbold` | `demo123` | Bank customer (account 100000001) |
| `sarnai` | `demo123` | Bank customer (account 100000002) |

Customer users come from dev-only seed data (`db/seed-dev`) and are linked to
banking customers via `customers.username`.

## Run Everything with Docker (new machine friendly)

The whole stack — 2 PostgreSQL, both backends and both frontends — runs with
one command. Only Docker Desktop is required on the host (no JDK/Maven/Node).

```powershell
docker compose up -d --build
```

| URL | Service |
| --- | --- |
| http://localhost:5173 | Registry portal |
| http://localhost:5174 | Banking app |
| http://localhost:8080/actuator/health | platform-api health |
| http://localhost:8084/swagger-ui/index.html | banking service Swagger |

Notes:

- The first build takes several minutes (Maven/npm downloads happen inside Docker).
- Dev-safe defaults (DB passwords, `JWT_SECRET`) are baked into
  `docker-compose.yml` for this demo stack; create a `.env` file to override
  them. Running the services manually (see below) still requires the env vars.
- Demo seed data is applied by default (see Demo Logins above).
- Stop with `docker compose down`; add `-v` to also reset database data.
- The frontends are static nginx builds: API base URLs are baked in at build
  time via the `VITE_API_BASE_URL` / `VITE_BANKING_API_URL` build args, so they
  assume the backends are reachable on `localhost:8080/8084` of the machine
  running the browser.

## Requirements (manual run without Docker)

- JDK 17+
- Maven 3.9+
- Docker Desktop
- Node.js 20+

This machine has a usable JDK at:

```text
C:\Users\basba\.jdk\jdk-17.0.16
```

## Environment Variables

Copy `.env.example` to `.env` and fill in the values. There are no in-app
defaults for passwords or `JWT_SECRET` — a service fails fast on startup if
one is missing. `JWT_SECRET` must be identical for platform-api and
banking-transfer-service (the platform issues tokens, banking validates them
with the same key).

Optional banking limits (defaults shown):

```text
BANKING_MAX_PER_TRANSFER=5000000.00
BANKING_DAILY_OUTGOING_TOTAL=10000000.00
```

Demo data (systems catalog, demo customers/accounts, demo customer users)
lives in dev-only `db/seed-dev` Flyway locations that are applied by default.
In production set `FLYWAY_LOCATIONS=classpath:db/migration` so only the
schema, reference data and admin user are applied.

## Run Databases

Starts both PostgreSQL containers: registry (5432) and banking (5433).

```powershell
docker compose up -d
```

## Run Backend

```powershell
$env:JAVA_HOME='C:\Users\basba\.jdk\jdk-17.0.16'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:POSTGRES_PASSWORD='registry_password'
$env:JWT_SECRET='change-this-dev-only-secret-at-least-32-characters'
cd backend/platform-api
mvn spring-boot:run
```

Health check:

```text
http://localhost:8080/actuator/health
```

## Run Banking Transfer Service

Uses the banking PostgreSQL (5433, `banking_transfer` DB) from docker-compose.

```powershell
$env:JAVA_HOME='C:\Users\basba\.jdk\jdk-17.0.16'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:BANKING_DB_PASSWORD='banking_password'
$env:JWT_SECRET='change-this-dev-only-secret-at-least-32-characters'
cd backend/banking-transfer-service
mvn spring-boot:run
```

Swagger UI:

```text
http://localhost:8084/swagger-ui/index.html
```

## Run Registry Portal (frontend)

```powershell
cd frontend
npm.cmd install
npm.cmd run dev -- --host 127.0.0.1 --port 5173
```

```text
http://127.0.0.1:5173/dashboard
```

## Run Banking App (frontend-banking)

Separate web app on its own port. Logs in against platform-api (8080) and
talks to the banking service (8084).

```powershell
cd frontend-banking
npm.cmd install
npm.cmd run dev
```

```text
http://127.0.0.1:5174/
```

Log in as `batbold`/`demo123` for the customer view or `admin`/`admin123`
for the teller view (customers, account management, audit log, reversals).
