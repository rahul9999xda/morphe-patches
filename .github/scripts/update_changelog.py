#!/usr/bin/env python3
"""Prepend a release-notes entry to CHANGELOG.md without duplicating versions."""
from __future__ import annotations

import re
import sys
from datetime import datetime, timezone
from pathlib import Path


def main() -> None:
    if len(sys.argv) != 5:
        raise SystemExit(
            "Usage: update_changelog.py VERSION PREVIOUS_TAG RELEASE_NOTES CHANGELOG"
        )

    version, previous_tag, notes_path, changelog_path = sys.argv[1:]
    notes_file = Path(notes_path)
    changelog_file = Path(changelog_path)
    notes = notes_file.read_text(encoding="utf-8").strip()

    # Remove the generated release-note title and administrative footer/metadata;
    # CHANGELOG.md should contain the human-facing changes only.
    lines = notes.splitlines()
    if lines and re.match(rf"^##\s+Release\s+{re.escape(version)}\s*$", lines[0]):
        lines = lines[1:]
    while lines and not lines[0].strip():
        lines.pop(0)

    cleaned: list[str] = []
    for line in lines:
        if line.strip() == "### Changes / Fixes":
            continue
        if line.strip() == "### Release information":
            break
        if line.strip() == "This release was built automatically by the repository release workflow.":
            break
        cleaned.append(line.rstrip())

    body = "\n".join(cleaned).strip()
    date = datetime.now(timezone.utc).date().isoformat()
    heading = f"## {version} ({date})"
    entry = heading + ("\n\n" + body if body else "")

    existing = changelog_file.read_text(encoding="utf-8") if changelog_file.exists() else ""
    # Idempotent: replace an existing entry for this version instead of duplicating it.
    text = existing.lstrip("\ufeff\n\r ")
    pattern = re.compile(
        rf"(?ms)^##\s+(?:\[[^\]]+\]\([^\n]*\)|Release\s+)?{re.escape(version)}(?:\s+\([^\n]*\))?\s*$.*?(?=^##\s+|\Z)"
    )
    match = pattern.search(text)
    if match:
        text = text[:match.start()] + text[match.end():]
        text = text.lstrip("\n")

    changelog_file.write_text(entry + "\n\n" + text.rstrip() + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
