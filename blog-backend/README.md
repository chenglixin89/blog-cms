# blog-backend

Spring Boot 3 backend for the blog CMS.

## Quick start (local development)

The default Spring profile is `dev`, which provides safe-but-weak defaults so the
project runs out of the box on a fresh clone:

```bash
mvn spring-boot:run
```

This starts the server on `http://localhost:8080` with:

- DB: `jdbc:mysql://localhost:3306/blog_cms` (created automatically), user `root`, password `123456`
- Default admin (seeded only when the `blog_admin_account` table is empty): `admin` / `admin123`
- JWT secret: a known dev placeholder

You can override any of these via environment variables (see below) without
editing the YAML files.

## Configuration

All sensitive values are read from environment variables with placeholder
fallbacks. See `src/main/resources/application.yml` for the full list. The
ones you most likely want to set are:

| Variable | Purpose | Required in prod |
| --- | --- | --- |
| `BLOG_DB_URL` | JDBC URL | Yes |
| `BLOG_DB_USERNAME` / `BLOG_DB_PASSWORD` | DB credentials | Yes |
| `BLOG_ADMIN_USERNAME` / `BLOG_ADMIN_PASSWORD` | Default admin (seeded only on empty DB) | Yes |
| `BLOG_JWT_SECRET` | HS256 signing key, **>= 32 bytes** | Yes |
| `BLOG_JWT_EXPIRE_SECONDS` | Token lifetime in seconds (default 7200) | No |
| `BLOG_CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed origins | Yes |
| `BLOG_UPLOAD_DIR` | Filesystem path for uploaded media | No |
| `BLOG_SQL_INIT_MODE` | `always` (dev) or `never` (prod) | No |
| `SPRING_PROFILES_ACTIVE` | `prod` to disable dev defaults | Yes |

Generate a strong JWT secret with:

```bash
openssl rand -base64 48
```

## Profiles

- `application.yml` &mdash; base config; reads env vars only, no hard-coded secrets.
- `application-dev.yml` &mdash; activated by default when no profile is set.
  Provides local convenience defaults. Never use in production.
- `application-prod.yml.example` &mdash; copy to `application-prod.yml` (already
  in `.gitignore`) and fill in real values, **or** set the matching env vars
  and start the app with `SPRING_PROFILES_ACTIVE=prod`.

## Endpoints

| Method | Path | Auth |
| --- | --- | --- |
| POST | `/api/admin/login` | none |
| `*` | `/api/admin/**` | Bearer JWT (validated by `AdminAuthInterceptor`) |
| `*` | `/api/front/**` | varies; see individual controllers |

The login endpoint returns a JWT in the response body. Pass it on subsequent
admin requests as `Authorization: Bearer <token>`.

## Password hashing

Admin passwords are stored as BCrypt hashes in `blog_admin_account.password_hash`.
On startup, any rows still holding the legacy `{noop}<plaintext>` format are
automatically rehashed to BCrypt; runtime login no longer accepts `{noop}`.
