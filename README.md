# LiveText — Real-time Collaborative Text Editor

A real-time collaborative text editor built with **Spring Boot** (Java 21) and **React** (Vite + TypeScript + Tailwind). Real-time collaboration uses **Yjs (CRDT)** with a relay-based architecture: the server persists updates and broadcasts them via **Redis pub/sub** without needing a server-side Yjs engine.

---

## Tech stack

| Layer    | Technology |
|----------|-----------|
| Backend  | Spring Boot 3.5, Java 21, Spring Security (JWT), Spring Data JPA, Flyway, Spring WebSocket, Spring Data Redis |
| Database | PostgreSQL 16 |
| Cache/PubSub | Redis 7 (cross-instance WebSocket broadcast) |
| Object storage | MinIO (image assets; backend-proxied upload + presigned-URL downloads) |
| Frontend | React 19, Vite, TypeScript, Tailwind CSS, Quill, Yjs, y-websocket, y-quill |
| Realtime | Yjs CRDT (relay architecture — server persists/broadcasts, clients merge) |
| Runtime  | Docker Compose |

---

## Quick start (Docker — recommended)

Requires only **Docker** (with Compose v2).

```bash
# 1. (Optional) configure environment — defaults already work out of the box
cp .env.example .env

# 2. Build and start everything (Postgres + Redis + MinIO + backend + frontend)
docker compose up --build
```

Once running:

- **Frontend (UI):** http://localhost:8081
- **Backend (API):** http://localhost:8080/api
- **PostgreSQL:** localhost:5432 (user/pass/db = `livetext`/`livetext`/`livetext`)
- **MinIO (S3 API):** http://localhost:9000 · **Console:** http://localhost:9001 (default `minioadmin`/`minioadmin`)

The frontend nginx container reverse-proxies `/api` and `/ws` to the backend, so the SPA talks same-origin — no extra URL configuration needed.

### Demo accounts (seeded on first start)

On first startup (empty DB), three demo users are created automatically:

| Username | Password     | Documents owned            |
|----------|-------------|----------------------------|
| `alice`  | `password123` | Meeting Notes, Project Roadmap |
| `bob`    | `password123` | Brainstorm Ideas           |
| `charlie`| `password123` | (none)                     |

Log in at http://localhost:8081 with any of these to explore the app immediately.
To disable seeding, set `SEED_DATA=false` in `.env`.

### Try the auth endpoints

```bash
# Register
curl -i -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"password123"}'

# Login (username OR email both work)
curl -i -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"alice","password":"password123"}'

# Current user (use the access token returned above)
curl -i http://localhost:8081/api/users/me \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

---

## Dev mode (no Docker)

Run only Postgres in Docker, then the backend and frontend natively for hot reload:

```bash
# 1. Start just the database
docker compose up db

# 2. Backend (from ./backend)
./mvnw spring-boot:run

# 3. Frontend (from ./frontend)
npm install
npm run dev      # http://localhost:5173
```

The Vite dev server proxies `/api` and `/ws` to `http://localhost:8080`, so the frontend uses the same relative URLs in both Docker and dev mode.

---

## Project layout

```
LiveText/
├── docker-compose.yml        # Postgres + Redis + MinIO + backend + frontend
├── .env.example              # environment variables (copy to .env)
├── backend/                  # Spring Boot app
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/example/backend/
│       │   ├── config/       # SecurityConfig, BeanConfig, CorsProperties, JwtProperties, SeedDataRunner, RedisConfig, MinioConfig, MinioProperties
│       │   ├── common/       # ApiResponse, GlobalExceptionHandler
│       │   ├── entity/       # JPA entities (User, Document, DocumentPermission, DocumentVersion, Asset, CollabState, CollabUpdate, Role)
│       │   ├── repository/   # Spring Data JPA repositories (one per entity)
│       │   ├── dto/          # Request/response DTOs grouped by domain (auth/, document/, user/, version/, asset/)
│       │   ├── exception/   # Domain-specific exceptions (UserNotFoundException, PermissionDeniedException, ...)
│       │   ├── service/      # Business logic services (AuthService, DocumentService, PermissionService, VersionService, AssetService, CollabStateService, JwtService, UserService)
│       │   ├── controller/   # REST controllers (Auth, User, Document, Permission, Version, Asset, Collab)
│       │   ├── security/     # Spring Security infra (JwtAuthFilter, AuthenticatedUser, UserDetailsServiceImpl, entry/denied handlers)
│       │   └── collab/       # WebSocket relay layer (YjsWebSocketHandler, YjsHandshakeInterceptor, SessionRegistry, RedisBroadcaster, YjsProtocol, Varint, WebSocketConfig)
│       └── resources/
│           ├── application.properties
│           └── db/migration/ # Flyway migrations (V1–V5)
└── frontend/                 # React + Vite app
    ├── Dockerfile
    ├── nginx.conf            # SPA + /api + /ws reverse proxy
    └── src/
```

---

## Configuration

The backend is fully env-driven (see `backend/src/main/resources/application.properties`):

| Variable            | Default                                                              | Description |
|---------------------|----------------------------------------------------------------------|-------------|
| `DB_URL`            | `jdbc:postgresql://localhost:5432/livetext`                          | JDBC URL |
| `DB_USERNAME`       | `livetext`                                                           | DB user |
| `DB_PASSWORD`       | `livetext`                                                           | DB password |
| `JWT_SECRET`        | dev default (32+ bytes)                                              | HS256 signing key — **change in production** |
| `CORS_ORIGINS`      | `http://localhost:5173,http://localhost:8081`                        | Allowed origins |
| `APP_COOKIE_SECURE` | `false`                                                              | Refresh-cookie `Secure` flag (set `true` behind HTTPS) |
| `SEED_DATA`         | `true`                                                               | Seed demo users on empty DB (dev profile) |
| `SPRING_PROFILES_ACTIVE` | `dev`                                                           | Active profiles (`dev` enables seed data) |
| `REDIS_HOST`         | `redis`                                                              | Redis host for pub/sub broadcast |
| `REDIS_PORT`         | `6379`                                                               | Redis port |
| `MINIO_ENDPOINT`     | `http://minio:9000` (Compose) / `http://localhost:9000` (local)      | Internal MinIO endpoint for server-side IO |
| `MINIO_PUBLIC_ENDPOINT` | `http://localhost:9000`                                         | Browser-reachable endpoint for presigned download URLs |
| `MINIO_ACCESS_KEY`   | `minioadmin` (`MINIO_ROOT_USER`)                                     | MinIO access key |
| `MINIO_SECRET_KEY`   | `minioadmin` (`MINIO_ROOT_PASSWORD`)                                 | MinIO secret key |
| `MINIO_BUCKET`       | `livetext-assets`                                                    | Bucket for image assets (auto-created on first upload) |
| `MINIO_PRESIGN_EXPIRY` | `10m`                                                              | Expiry for presigned GET URLs |

---

## Development commands

```bash
# Backend tests (unit + integration, use in-memory H2, no DB needed)
cd backend && ./mvnw test

# Frontend lint + typecheck / build
cd frontend && npm run lint
cd frontend && npm run build
```
