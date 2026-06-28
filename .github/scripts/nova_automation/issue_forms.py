#!/usr/bin/env python3
"""Keep GitHub issue-form workflow metadata aligned and non-duplicated."""

from __future__ import annotations

import argparse
import difflib
from pathlib import Path
from typing import Iterable

from .project_metadata import SIZE_OPTIONS, priority_options, status_options

SYNCED_FORM_FIELDS = {
    "priority": "Priority",
    "size": "Size",
    "suggested-status": "Suggested status",
}

FORBIDDEN_FORM_FIELDS = {
    "milestone": "Milestone",
    "labels": "Labels",
}


class IssueFormError(RuntimeError):
    """Raised when issue forms violate the shared metadata policy."""



def shared_options() -> dict[str, tuple[str, ...]]:
    """Build issue-form option lists from the shared metadata source of truth."""

    return {
        "priority": priority_options(),
        "size": SIZE_OPTIONS,
        "suggested-status": status_options(),
    }



def field_block_bounds(lines: list[str], field_id: str) -> tuple[int, int] | None:
    """Return the line range for one issue-form field block."""

    index = 0
    while index < len(lines):
        if lines[index] != f"    id: {field_id}\n":
            index += 1
            continue
        start = index
        while start > 0 and not lines[start].startswith("  - type: "):
            start -= 1
        end = index + 1
        while end < len(lines) and not lines[end].startswith("  - type: "):
            end += 1
        return start, end
    return None



def forbidden_fields_in_text(text: str) -> list[str]:
    """Return forbidden issue-form body fields present in one file."""

    lines = text.splitlines(keepends=True)
    return [field_name for field_id, field_name in FORBIDDEN_FORM_FIELDS.items() if field_block_bounds(lines, field_id)]



def option_lines(values: Iterable[str]) -> list[str]:
    """Render one dropdown options block."""

    return [f"        - {value}\n" for value in values]



def replace_field_options(text: str, field_id: str, values: Iterable[str]) -> str:
    """Replace the options list for one issue-form dropdown field."""

    lines = text.splitlines(keepends=True)
    result = lines[:]
    block = field_block_bounds(result, field_id)
    if block is None:
        raise IssueFormError(f"Missing required shared field '{field_id}'")

    start, end = block
    options_index = None
    for candidate in range(start, end):
        if result[candidate] == "      options:\n":
            options_index = candidate
            break
    if options_index is None:
        raise IssueFormError(f"Field '{field_id}' has no options block")

    options_end = options_index + 1
    while options_end < end and result[options_end].startswith("        -"):
        options_end += 1

    result[options_index + 1:options_end] = option_lines(values)
    return "".join(result)



def sync_form_text(text: str, options: dict[str, tuple[str, ...]]) -> str:
    """Return issue-form text with all synchronized option blocks updated."""

    forbidden = forbidden_fields_in_text(text)
    if forbidden:
        fields = ", ".join(forbidden)
        raise IssueFormError(
            f"Remove duplicated native GitHub field(s) from issue form body: {fields}"
        )

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

    options = shared_options()
    changed = False
    failed = False
    for path in issue_form_paths(root):
        before = path.read_text(encoding="utf-8")
        try:
            after = sync_form_text(before, options)
        except IssueFormError as error:
            print(f"::error file={path}::{error}")
            failed = True
            continue
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

    if failed:
        return 1
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
    raise SystemExit(main())
