#!/usr/bin/env python3
"""Keep GitHub issue-form shared options aligned with project automation constants."""

from __future__ import annotations

import argparse
import difflib
import importlib.util
import sys
from pathlib import Path
from types import ModuleType
from typing import Iterable

SHARED_FORM_FIELDS = {
    "milestone": "Milestone",
    "labels": "Labels",
    "priority": "Priority",
    "size": "Size",
    "suggested-status": "Suggested status",
}

SIZE_OPTIONS = ("XS", "S", "M", "L", "XL")


def load_project_automation(root: Path) -> ModuleType:
    """Load the project automation module without requiring a Python package."""

    script = root / ".github" / "scripts" / "project_automation.py"
    spec = importlib.util.spec_from_file_location("nova_project_automation", script)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Cannot load {script}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def unique_values(values: Iterable[str]) -> tuple[str, ...]:
    """Return unique strings while preserving their first-seen order."""

    ordered: list[str] = []
    seen: set[str] = set()
    for value in values:
        if value in seen:
            continue
        ordered.append(value)
        seen.add(value)
    return tuple(ordered)


def shared_options(root: Path) -> dict[str, tuple[str, ...]]:
    """Build issue-form option lists from the automation source of truth."""

    automation = load_project_automation(root)
    milestones = (
        automation.PROJECT_WORKFLOW_MILESTONE,
        automation.MVP_MILESTONE,
        *automation.ADVANCED_FEATURE_MILESTONES,
        automation.FUTURE_MILESTONE,
    )
    return {
        "milestone": tuple(milestones),
        "labels": tuple(entry[0] for entry in automation.MANAGED_KIND_LABELS.values()),
        "priority": unique_values(automation.PRIORITY_ALIASES.values()),
        "size": SIZE_OPTIONS,
        "suggested-status": unique_values(automation.STATUS_ALIASES.values()),
    }


def option_lines(field_id: str, values: Iterable[str]) -> list[str]:
    """Render one issue-form options block."""

    if field_id == "labels":
        return [f"        - label: {value}\n" for value in values]
    return [f"        - {value}\n" for value in values]


def replace_field_options(text: str, field_id: str, values: Iterable[str]) -> str:
    """Replace the options list for one issue-form field."""

    lines = text.splitlines(keepends=True)
    result = lines[:]
    index = 0
    while index < len(result):
        if result[index] != f"    id: {field_id}\n":
            index += 1
            continue

        next_field = index + 1
        while next_field < len(result) and not result[next_field].startswith("  - type: "):
            next_field += 1

        options_index = None
        for candidate in range(index + 1, next_field):
            if result[candidate] == "      options:\n":
                options_index = candidate
                break
        if options_index is None:
            raise ValueError(f"Field '{field_id}' has no options block")

        options_end = options_index + 1
        while options_end < next_field and result[options_end].startswith("        -"):
            options_end += 1

        result[options_index + 1:options_end] = option_lines(field_id, values)
        index = next_field

    return "".join(result)


def sync_form_text(text: str, options: dict[str, tuple[str, ...]]) -> str:
    """Return issue-form text with all shared option blocks synchronized."""

    synced = text
    for field_id, values in options.items():
        synced = replace_field_options(synced, field_id, values)
    return synced


def issue_form_paths(root: Path) -> list[Path]:
    """Return issue-form YAML files, excluding GitHub's template chooser config."""

    template_dir = root / ".github" / "ISSUE_TEMPLATE"
    return sorted(path for path in template_dir.glob("*.yml") if path.name != "config.yml")


def unified_diff(path: Path, before: str, after: str) -> str:
    """Return a unified diff for one changed file."""

    return "".join(difflib.unified_diff(
        before.splitlines(keepends=True),
        after.splitlines(keepends=True),
        fromfile=f"{path} (current)",
        tofile=f"{path} (expected)",
    ))


def sync_issue_forms(root: Path, check: bool) -> int:
    """Synchronize or check all issue-form shared options."""

    options = shared_options(root)
    changed = False
    for path in issue_form_paths(root):
        before = path.read_text(encoding="utf-8")
        try:
            after = sync_form_text(before, options)
        except ValueError as error:
            print(f"::error file={path}::{error}")
            return 1
        if before == after:
            print(f"{path}: already aligned")
            continue
        changed = True
        if check:
            print(f"::error file={path}::Issue-form shared options are not aligned")
            print(unified_diff(path, before, after))
        else:
            path.write_text(after, encoding="utf-8")
            print(f"{path}: updated")

    if changed and check:
        print("Run `python3 .github/scripts/issue_forms.py` to update issue-form shared options.")
        return 1
    return 0


def build_parser() -> argparse.ArgumentParser:
    """Build the command-line parser."""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", default=".", help="Repository root path")
    parser.add_argument("--check", action="store_true", help="Fail instead of editing files when drift is found")
    return parser


def main() -> int:
    """Run the issue-form synchronization command."""

    args = build_parser().parse_args()
    return sync_issue_forms(Path(args.root).resolve(), args.check)


if __name__ == "__main__":
    sys.exit(main())
