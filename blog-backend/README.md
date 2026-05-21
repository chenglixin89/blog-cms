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
| `BLOG_FLYWAY_ENABLED` | `true` (default) or `false` to disable migrations | No |
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

## Database migrations

Schema changes are managed by **Flyway**. Migration files live in:

```
src/main/resources/db/migration/   # *.sql
src/main/java/db/migration/        # *.java
```

Flyway runs automatically on every startup before the application context
finishes initialising. The current migration set:

| Version | Type | What it does |
| --- | --- | --- |
| `V1__init_schema.sql` | SQL | Full baseline schema (idempotent). |
| `V2__add_legacy_missing_columns.java` | Java | Adds columns missing from very old databases (idempotent). |
| `V3__seed_reference_data.sql` | SQL | Initial categories, tags, friend links, default site settings, default AI provider. |

For pre-existing databases (created by the old `DatabaseMigrationRunner`), the
`spring.flyway.baseline-on-migrate=true` flag is set, so Flyway will adopt the
existing schema as version 0 instead of failing. Subsequent migrations apply
incrementally.

Runtime data seeding (admin password BCrypt hashing, AI skill prompt updates,
demo content, legacy `{noop}` rehash) lives in `DataSeeder` rather than in a
migration, because those operations depend on runtime configuration or have
soft-delete-aware logic.

To add a new schema change:

```bash
# Pick the next free version number, e.g. V4, then create:
src/main/resources/db/migration/V4__add_search_index.sql
```

Naming rules: `V<version>__<snake_case_description>.<sql|java>`. Two underscores
between version and description.

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
