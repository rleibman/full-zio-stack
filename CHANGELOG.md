# Changelog

What each version of the template changes. `copier update` brings these changes into projects generated from an
earlier version.

## 0.1.0 (unreleased)

The first release as a [Copier](https://copier.readthedocs.io/) template.

**Choices:** full-stack or server-only; zio-http or http4s; MariaDB, MySQL, PostgreSQL or SQLite; Quill, doobie or
Slick; AI (langchain4j) or none; authentication (zio-auth) or none; Docker, Debian/systemd or no packaging; GitHub Actions or no CI; MIT, Apache-2.0, BSD-3-Clause, GPL-3.0 or
proprietary license.

**Generated projects have:**
- Scala 3.9, ZIO 2, sbt 2, compiled with `-Werror` and `-Yexplicit-nulls`.
- A shared `Repository[F[_]]` contract in the model: the server implements it with ZIO over the database, the
  Scala.js client with `AsyncCallback` over GraphQL. Errors are a shared `RepositoryError`.
- A Caliban GraphQL API (`/api/graphql`, GraphiQL at `/api/graphiql`), resolved directly against the repository, with
  error codes; `/health`; the client's static files with client-side-route fallback.
- Flyway migrations, run at startup, and a pooled DataSource that closes on shutdown.
- A Scala.js + scalajs-react + Material UI client (full-stack): tab navigation and a CRUD screen for the sample
  `ModelObject` entity, built with vite, with a GraphQL client generated from the server's schema.
- Tests: a generic CRUD contract run against an in-memory mock and a real database (testcontainers, or a temporary
  file for SQLite), an HTTP contract run against the real server, and a check that the committed GraphQL schema is
  current.
- Optional AI (langchain4j): an `AiService` and a `suggestDescription` GraphQL mutation, with Anthropic, OpenAI or
  Ollama chosen by configuration rather than at generation time.
- Optional authentication (zio-auth): login, registration with an emailed confirmation, password recovery and JWT
  sessions, over a `UserStore` of users and PBKDF2 password hashes, with the login screens in the client. It needs
  the zio-http server, and a `GITHUB_TOKEN` with `read:packages` to resolve zio-auth from GitHub Packages.
- OpenTelemetry tracing (zio-telemetry): a span per GraphQL operation, exported over OTLP when
  `app.telemetry.endpoint` (or `OTEL_EXPORTER_OTLP_ENDPOINT`) is set, and a no-op tracer otherwise.
- `AGENTS.md`, `CLAUDE.md` and an `add-entity` skill for AI agents.
- A development `docker-compose.yml`, a README, a LICENSE, and CI.

**Known limitation:** the client's ScalablyTyped facades need a locally published fork of the converter (sbt 2
support), so full-stack projects only build where it's installed, and their CI tests the server side only.
