#!/usr/bin/env python3
"""Keeps the AI docs honest.

  check-ai-docs.py static              in this repository: every copier.yml question is documented in the skill's
                                       answers.md (and nothing else is), the plugin manifests parse, and the skills
                                       have a name and description.
  check-ai-docs.py generated <dir>     in a generated project: every file path that AGENTS.md and the add-entity skill
                                       mention exists.

Exits non-zero, listing the problems, if anything is off.
"""

import json
import re
import sys
from pathlib import Path

import yaml

REPO = Path(__file__).resolve().parent.parent


def frontmatter(skill: Path) -> dict:
    text = skill.read_text()
    match = re.match(r"\A---\n(.*?)\n---\n", text, re.S)
    return yaml.safe_load(match.group(1)) if match else {}


def static_checks() -> list[str]:
    problems = []
    questions = {k for k in yaml.safe_load((REPO / "copier.yml").read_text()) if not k.startswith("_")}
    answers_doc = (REPO / "claude-plugin/skills/full-zio-stack/references/answers.md").read_text()
    documented = set(re.findall(r"`([a-z_]+)`", answers_doc))
    for question in sorted(questions - documented):
        problems.append(f"copier.yml question `{question}` is not documented in references/answers.md")
    # Table rows that name a key copier.yml doesn't have.
    for key in sorted(set(re.findall(r"^\| `([a-z_]+)` \|", answers_doc, re.M)) - questions):
        problems.append(f"references/answers.md documents `{key}`, which copier.yml doesn't ask")

    for manifest in [REPO / ".claude-plugin/marketplace.json", REPO / "claude-plugin/.claude-plugin/plugin.json"]:
        try:
            json.loads(manifest.read_text())
        except (OSError, json.JSONDecodeError) as e:
            problems.append(f"{manifest.relative_to(REPO)}: {e}")

    for skill in [REPO / "claude-plugin/skills/full-zio-stack/SKILL.md"]:
        meta = frontmatter(skill)
        for field in ["name", "description"]:
            if not meta.get(field):
                problems.append(f"{skill.relative_to(REPO)}: frontmatter has no {field}")
    return problems


# A backticked relative path to a file or directory, e.g. `server/src/main/resources/application.conf`.
PATH = re.compile(r"`([A-Za-z0-9_.-]+(?:/[A-Za-z0-9_.-]+)+/?)`")
# Placeholders the docs use on purpose: the example entity, and elided directories.
PLACEHOLDERS = ("Thing", "...", "…")


def generated_checks(project: Path) -> list[str]:
    problems = []
    docs = [project / "AGENTS.md", project / ".claude/skills/add-entity/SKILL.md"]
    for doc in docs:
        if not doc.exists():
            problems.append(f"{doc.relative_to(project)} is missing")
            continue
        for path in sorted(set(PATH.findall(doc.read_text()))):
            if any(p in path for p in PLACEHOLDERS) or path.startswith(("http", "classpath")):
                continue
            if not (project / path).exists():
                problems.append(f"{doc.relative_to(project)} mentions `{path}`, which doesn't exist")
    skill = project / ".claude/skills/add-entity/SKILL.md"
    if skill.exists():
        meta = frontmatter(skill)
        for field in ["name", "description"]:
            if not meta.get(field):
                problems.append(f"{skill.relative_to(project)}: frontmatter has no {field}")
    return problems


def main() -> None:
    if len(sys.argv) >= 2 and sys.argv[1] == "static":
        problems = static_checks()
    elif len(sys.argv) == 3 and sys.argv[1] == "generated":
        problems = generated_checks(Path(sys.argv[2]).resolve())
    else:
        sys.exit(__doc__)
    for problem in problems:
        print(f"ERROR: {problem}", file=sys.stderr)
    if problems:
        sys.exit(1)
    print("AI docs OK")


if __name__ == "__main__":
    main()
