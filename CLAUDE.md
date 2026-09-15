# full-zio-stack: working on the template

This repo is the source of the **full-zio-stack Copier template** and of its **Claude Code plugin**. It's under
construction; the plan (local and gitignored) is at `.claude/plans/copier-template.md`.

## What lives where

| Path | What it is |
|---|---|
| sbt modules (`model/`, `db-*/`, `server-*/`, `client/`, `stLib/`) | A normal, runnable sbt project containing **every** variant at once |
| `overlay/` | Hand-written Jinja files that exist only in generated projects: `build.sbt.jinja`, `AGENTS.md.jinja`, `.claude/skills/…`, etc. |
| `scripts/MakeTemplate.scala` | Generator: source modules + `overlay/` → `template/` |
| `template/` | **Generated. Never edit by hand.** Copier's `_subdirectory` |
| `copier.yml` | Template questions |
| `claude-plugin/` + `.claude-plugin/marketplace.json` | Claude Code plugin with the `full-zio-stack` skill, which generates projects from this template |
| `tests/` | Scripts that test what the template generates (see `tests/README.md`); `.github/workflows/ci.yml` runs them |

## Rules

- **The repo must stay a real sbt project.** No Jinja inside Scala sources; variants are whole modules or whole files.
- **Variants plug in by fully qualified name.** Every DB layer provides `…db.ZIORepository.live` with the same
  signature, and every HTTP server provides `…server.Main`. Keep signatures identical across variants.
- **One repository contract.** `Repository[F[_]]` and the per-entity `...Operations[F]` live in `model` (shared). The
  server implements them with ZIO, the client with `AsyncCallback`. No pass-through service objects: resolvers call the
  repository directly.
- **Sentinels:** in module sources, `net.leibman.fullziostack`, `FullZIOStack`, `full-zio-stack` and `Full ZIO Stack`
  are rewritten to the user's answers. Don't use those strings for anything that must survive generation. Refer to
  the template repo itself (its URL) only from `overlay/` files, which are not sentinel-processed.
- **After changing sources or `overlay/`,** regenerate `template/` and commit both together (CI fails if they differ).
- Build with `sbt --error …`; drop `--error` only when you need the details. `sbt test` is incremental in sbt 2:
  use `sbt testFull` (or `sbt testServerSide`, everything but the client) when you need everything to run.
- **Test generated projects** after changing anything that ends up in them: `tests/generate-and-build.sh` for a
  combination (`WITH_CLIENT=1` for full-stack), and `scala-cli run --server=false scripts/MakeTemplate.scala` first.

## Keep the AI docs in sync (CI checks this)

These files are how Claude learns to use the template and the projects it generates. They are part of the product.

| When you change… | Also update… |
|---|---|
| a question in `copier.yml` (added, removed, renamed, new values or defaults) | `claude-plugin/skills/full-zio-stack/references/answers.md` and, if it affects generation or prerequisites, `SKILL.md` |
| a file path, class name or command that `ModelObject` uses | `overlay/.claude/skills/add-entity/SKILL.md.jinja` |
| conventions, commands, modules or compiler workarounds | `overlay/AGENTS.md.jinja` |
| the plugin's behaviour | the `version` in `claude-plugin/.claude-plugin/plugin.json` |
