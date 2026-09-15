---
name: full-zio-stack
description: This skill should be used when the user asks to "build a full stack app with this stack", "create a new full-zio-stack project", "scaffold a ZIO app", "start a new Scala 3 / ZIO / Scala.js project", "new app with my stack", or any request to create a new application on the Scala 3 + ZIO 2 + Caliban GraphQL + Scala.js/React/MUI stack (zio-http or http4s; Quill, doobie or Slick; MariaDB, MySQL, Postgres or SQLite). It generates the project from the full-zio-stack Copier template instead of writing the scaffolding by hand, then verifies it builds and runs. Also use it to update an existing project generated from that template ("update from the template", "copier update").
version: 0.1.0
---

# full-zio-stack: new projects from the Copier template

The template at `gh:rleibman/full-zio-stack` generates a complete, tested project: Scala 3 + ZIO 2, a Caliban
GraphQL API, Flyway migrations, a database layer, and (optionally) a Scala.js + React + Material UI client with a
working CRUD screen. **Never hand-write this scaffolding.** Generate it, verify it, then build the user's features on top.

## 1. Check the template is available

```bash
git ls-remote --tags https://github.com/rleibman/full-zio-stack 'v*'
```

- **Tags listed:** use `gh:rleibman/full-zio-stack` (latest tag), or `--vcs-ref vX.Y.Z` if the user asks for a version.
- **No tags yet:** the template is still under construction. If `~/projects/full-zio-stack/copier.yml` exists, the
  user can opt into the development version: source `~/projects/full-zio-stack` with `--vcs-ref HEAD`.
  Otherwise, tell the user the template isn't released yet and stop.
- **The user names a path or version:** use exactly that.

## 2. Work out the answers

Read `references/answers.md` for every question, its allowed values, its default, and the phrases that map to it.

- Infer as much as possible from the request ("REST backend only" → `components=server-only`,
  "Postgres" → `database=postgres`, "no docker" or "embedded" → `database=sqlite`, "cats" → `http_server=http4s`
  and usually `db_layer=doobie`).
- Take `author_name`/`author_email` from `git config user.name` / `git config user.email`.
- Anything not implied keeps the template default (zio-http, Quill, MariaDB, full-stack, MIT, Docker packaging,
  GitHub Actions).
- `project_name` and `organization` rarely have good defaults. If the user didn't give them, ask.
- **Confirm once, in a single message:** show the resolved answers as a compact list and ask the user to confirm or
  change them. Don't ask about each question separately.

## 3. Check prerequisites for those answers

| Needed for | Check | If missing |
|---|---|---|
| always | `copier --version`, else `uvx copier --version`, else `pipx run --spec copier copier --version` | ask the user to install one (`pipx install copier` or `uv tool install copier`) |
| always | `sbt --version`, `java -version` (17+) | stop and tell the user |
| `components=full-stack` | `node --version`, `npm --version` | stop, or offer `server-only` |
| `components=full-stack` | `ls ~/.ivy2/local/org.scalablytyped.converter/sbt-converter_sbt2_3/` shows the version pinned in the generated `stLib/project/plugins.sbt` | the ScalablyTyped converter isn't available on this machine yet (it's a locally published fork); offer `server-only` |
| `dev_compose=true` | `docker compose version` | set `dev_compose=false` and tell the user they need their own database |

## 4. Generate

Run non-interactively. Use `COPIER` for whichever form worked in step 3:

```bash
$COPIER copy --trust --defaults \
  --data project_name="Acme Orders" \
  --data organization=com.acme \
  --data author_name="…" --data author_email="…" \
  --data http_server=zio-http --data database=mariadb --data db_layer=quill \
  gh:rleibman/full-zio-stack ./acme-orders
```

- Pass every answer you resolved as `--data key=value`. `--defaults` fills in the rest without prompting.
- `--trust` is required because the template runs post-generation tasks (e.g. `git init`). It's fine here because
  this is the user's own template. Never pass `--trust` for other templates without asking.
- The destination must not exist or must be empty. Never generate into an existing project.

## 5. Verify: don't report success until this passes

In the generated directory:

1. Read `AGENTS.md`. It describes this project's modules, commands and conventions for the chosen variants.
2. Full-stack only: `cd stLib && npm install && sbt --error publishLocal`. The first run takes about 5–6 minutes and
   up to 6 GB of memory, so run it in the background.
3. If `docker-compose.yml` exists: `docker compose up -d`, then wait until the database is healthy.
4. `sbt --error test`.
5. Start `sbt server/run` in the background, then check `curl -fsS localhost:<http_port>/health` and one GraphQL
   query (`curl -fsS -H 'Content-Type: application/json' -d '{"query":"{ __typename }"}' localhost:<http_port>/api/graphql`).
   Stop the server afterwards.
6. Full-stack only: `sbt --error client/webDist`.

If a step fails, fix it and say what you fixed. If the failure is a template bug (it would affect every generated
project), tell the user so it can be fixed in the template.

## 6. Build what the user actually asked for

The generated app contains a sample entity, `ModelObject`, wired through every layer.

- For each domain entity the user wants, use the generated project's `add-entity` skill
  (`.claude/skills/add-entity/SKILL.md`). It lists the exact files that make up `ModelObject` for this variant and
  how to mirror them.
- Once the user's first entity works end to end, offer to remove the `ModelObject` sample (also described in that skill).

## Updating a project generated from the template

1. The working tree must be clean (`git status`). If it isn't, ask the user to commit or stash; don't commit for them.
2. `$COPIER update --trust --defaults` (add `--vcs-ref vX.Y.Z` to target a version). To switch a variant, e.g. the
   HTTP server, add `--data http_server=http4s`.
3. Resolve any git conflict markers. Copier leaves them inline, and the files show as unmerged.
4. Run the verification steps from section 5 again.

Never edit `.copier-answers.yml` by hand. Change answers with `copier update --data`.
