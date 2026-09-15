# Testing the template

The sbt build tests the *source* (every variant's contract suites: `sbt testFull`, or `sbt testServerSide` for
everything but the client). The scripts here test what the template *generates*.

| Script | What it proves | Typical run |
|---|---|---|
| `generate-and-build.sh <dir> [--data k=v ...]` | A generated project's AI docs name only real files, `sbt testFull` passes, the server answers `/health` and GraphQL, and its packaging builds. With `WITH_CLIENT=1` (full-stack only), it also builds the client and checks it in headless Chrome with `ui-check.mjs`. | `tests/generate-and-build.sh /tmp/p --data database=postgres` |
| `check-ai-docs.py static` | Every `copier.yml` question is documented in the skill's `answers.md`, the plugin manifests parse, and skills have a name and description. | CI |
| `check-ai-docs.py generated <dir>` | Every file path the generated `AGENTS.md` and `add-entity` skill mention exists. | run by `generate-and-build.sh` |
| `validators.sh` | Invalid answers (bad package, missing author, unknown choices...) are rejected, each by the right question. | `tests/validators.sh` |
| `update-roundtrip.sh <from> <to> [--data ...]` | A project generated from template version `<from>` updates to `<to>` without conflicts and still builds. | CI, once there are two `v*` tags |
| `ui-check.mjs [url] [screenshot dir]` | In headless Chrome: the page renders, a ModelObject can be created through the dialog and shows up in the table, no console errors. | `node tests/ui-check.mjs` |

All scripts use `copier` from the `PATH`; set `COPIER="pipx run --spec copier copier"` (or `uvx copier`) if it isn't
installed. They generate from this repository, uncommitted changes included, unless `TEMPLATE` points elsewhere.

Locally, pass `--data db_port=...` when the default database port (3306, or 5432 for Postgres) is already in use.

## What CI doesn't cover

- **The client.** Building it needs the ScalablyTyped facades from `stLib/`, and those need a locally published build
  of the converter. Before a release, run at least:

  ```bash
  WITH_CLIENT=1 tests/generate-and-build.sh /tmp/full --data project_name="Acme Store" --data organization=com.acme
  ```

  (The first time for a given name and organization, publish the generated project's facades first:
  `cd /tmp/full/stLib && npm install && sbt --error publishLocal`, which takes several minutes.)

- **Claude using the template.** Before each release, in a fresh Claude Code session with the plugin installed
  (`/plugin marketplace add rleibman/full-zio-stack`, then `/plugin install full-zio-stack@full-zio-stack`, or a symlink
  from `~/.claude/skills/full-zio-stack` to `claude-plugin/skills/full-zio-stack`):
  1. Ask: "build a full stack app called Smoke Test with sqlite, server only".
     - [ ] It confirms the resolved answers once, in a single message.
     - [ ] It checks the prerequisites, generates with `copier copy`, and runs the verification steps (tests, `/health`,
           a GraphQL query) before reporting success.
  2. Ask: "add a Customer entity with a name and an email".
     - [ ] It follows `.claude/skills/add-entity/SKILL.md`, adding the model, repository operations, migration, tests
           and API (and the client screen, for full-stack), and `sbt --error testFull` passes.
  3. Note anything it got wrong or had to guess, and fix the skill or `AGENTS.md`.
