#!/usr/bin/env python3
"""Generate Morphe-style Conventional Commit release notes and CHANGELOG.md entry."""
import os
import re
import subprocess
import sys
from collections import OrderedDict

CATEGORIES = OrderedDict([
    ("🐛 Bug Fixes", {"fix"}),
    ("✨ New Features", {"feat", "feature"}),
    ("🚀 Updated App Support", {"bump"}),
    ("🔧 Improvements", {"perf"}),
    ("🏗️ Build / Compatibility", {"build"}),
])


def git(*args):
    return subprocess.check_output(["git", *args], text=True).strip()


def main():
    if len(sys.argv) != 4:
        raise SystemExit("Usage: generate_release_notes.py VERSION PREVIOUS_TAG OUTPUT")
    version, previous, output = sys.argv[1:]
    repo = os.environ.get("GITHUB_REPOSITORY", "rahul9999xda/morphe-patches")
    if previous:
        log_range = f"{previous}..HEAD"
        compare = f"https://github.com/{repo}/compare/{previous}...v{version}"
        raw = git("log", "--format=%H%x1f%s%x1f%b%x1e", log_range)
    else:
        compare = f"https://github.com/{repo}/commits/v{version}"
        raw = git("log", "--format=%H%x1f%s%x1f%b%x1e")

    buckets = OrderedDict((name, []) for name in CATEGORIES)
    other = []
    for record in raw.split("\x1e"):
        record = record.strip()
        if not record:
            continue
        parts = record.split("\x1f", 2)
        if len(parts) < 2:
            continue
        sha, subject = parts[0], parts[1]
        body = parts[2] if len(parts) > 2 else ""
        if re.match(r"^chore:\s*Release\s+v", subject, re.I):
            continue
        match = re.match(r"^([a-zA-Z]+)(?:\(([^)]*)\))?(!)?:\s*(.+)$", subject)
        if not match:
            continue
        typ, scope, breaking, description = match.groups()
        typ = typ.lower()
        if breaking or "BREAKING CHANGE:" in body:
            category = "✨ New Features"
            description = f"**BREAKING:** {description}"
        else:
            category = next((name for name, types in CATEGORIES.items() if typ in types), None)
        if not category:
            continue
        short = sha[:7]
        scope_text = f"**{scope}:** " if scope else ""
        item = f"{scope_text}{description} ([{short}](https://github.com/{repo}/commit/{sha}))"
        (buckets[category] if category in buckets else other).append(item)

    lines = [f"## [{version}]({compare}) ({git('log', '-1', '--format=%cs')})"]
    any_items = False
    for category, items in buckets.items():
        if items:
            any_items = True
            lines += [f"### {category}", ""]
            lines += [f"* {item}" for item in dict.fromkeys(items)]
            lines.append("")
    if other:
        any_items = True
        lines += ["### 📌 Other Changes", ""]
        lines += [f"* {item}" for item in dict.fromkeys(other)]
        lines.append("")
    if not any_items:
        lines += ["### 🔧 Improvements", "", "* Maintenance release.", ""]
    release_notes = "\n".join(lines).rstrip() + "\n"
    with open(output, "w", encoding="utf-8") as f:
        f.write(release_notes)

    changelog = "CHANGELOG.md"
    old = ""
    if os.path.exists(changelog):
        with open(changelog, encoding="utf-8") as f:
            old = f.read().strip()
    with open(changelog, "w", encoding="utf-8") as f:
        f.write(release_notes + ("\n" + old + "\n" if old else ""))


if __name__ == "__main__":
    main()
