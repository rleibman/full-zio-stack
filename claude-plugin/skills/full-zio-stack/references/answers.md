# Template questions

This list must stay in sync with the template's `copier.yml`; CI checks that every question appears here.
Pass values with `--data key=value`. Questions marked *derived* have good defaults computed from earlier answers,
so only override them when the user explicitly asks.

## Identity

| Key | Values | Default | Phrases that imply it / notes |
|---|---|---|---|
| `project_name` | free text | `My ZIO App` | "an app called X", "for X". Ask if absent. |
| `project_slug` | kebab-case | *derived* from `project_name` | directory, sbt and npm name |
| `class_prefix` | PascalCase | *derived* from `project_name` | prefix for generated class names |
| `organization` | reverse domain | `com.example` | "for acme.com" → `com.acme`. Ask if absent. |
| `base_package` | Scala package | *derived*: `<organization>.<slug without dashes>` | must match `^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*$` |
| `author_name` | free text | none | `git config user.name` |
| `author_email` | free text | empty | `git config user.email` |
| `license` | `MIT`, `Apache-2.0`, `BSD-3-Clause`, `GPL-3.0`, `Proprietary` | `MIT` | "closed source", "internal", "company project" → `Proprietary` |
| `description` | free text | empty | one line from the request |
| `repo_url` | URL | empty | only if the user gives one |

Computed, never asked: `base_package_path` (from `base_package`), `copyright_year` (the year of generation, kept on
updates), `migration_dir` (`mysql` for MariaDB and MySQL, otherwise the database).

## Shape

| Key | Values | Default | Phrases |
|---|---|---|---|
| `components` | `full-stack`, `server-only` | `full-stack` | "API only", "backend", "no UI", "headless", "microservice" → `server-only` |

## Server

| Key | Values | Default | Phrases |
|---|---|---|---|
| `http_server` | `zio-http`, `http4s` | `zio-http` | "http4s", "cats", "typelevel" → `http4s` |
| `database` | `mariadb`, `mysql`, `postgres`, `sqlite` | `mariadb` | "Postgres"/"PostgreSQL" → `postgres`; "MySQL" → `mysql`; "SQLite", "embedded", "file-based", "no database server", "no docker" → `sqlite` |
| `db_layer` | `quill`, `doobie`, `slick` | `quill` | "doobie", "plain SQL", "cats" → `doobie`; "Slick" → `slick` |
| `http_port` | integer | `8080` | "on port N" |
| `db_name` | identifier | *derived* from `project_slug` | not asked when `database=sqlite` |
| `db_user` | identifier | *derived*: same as `db_name` | also the development password; not asked when `database=sqlite` |
| `db_port` | integer | 3306, or 5432 for Postgres | host port of the development database; change it if that port is taken locally. Not asked when `database=sqlite` |
| `sqlite_file` | path | `data/<project_slug>.db` | only when `database=sqlite` |

## Ops

| Key | Values | Default | Phrases |
|---|---|---|---|
| `dev_compose` | `true`, `false` | `true` | docker-compose for a dev database; not asked when `database=sqlite`. Set `false` if Docker is unavailable. |
| `packaging` | `none`, `docker`, `systemd` | `docker` | "deploy as a .deb", "systemd service" → `systemd`; "no packaging" → `none` |
| `ci` | `github-actions`, `none` | `github-actions` | "no CI", or a non-GitHub host → `none` |
| `scala_steward` | `true`, `false` | `true` | only when `ci=github-actions` |
| `git_init` | `true`, `false` | `true` | `false` if the destination is inside an existing git repository |

## Combinations that work well

- **Cats-leaning:** `http_server=http4s`, `db_layer=doobie` (they share cats-effect).
- **Zero setup / demo:** `database=sqlite` (no Docker needed), `components=server-only` if the ScalablyTyped
  converter isn't available.
- **Default ZIO-native:** `zio-http` + `quill` + `mariadb`.
