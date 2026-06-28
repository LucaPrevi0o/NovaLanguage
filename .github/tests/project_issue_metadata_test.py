#!/usr/bin/env python3
"""Unit tests for Nova's issue metadata sync helpers."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path
from typing import Any
from unittest.mock import patch

SCRIPTS_DIR = Path(__file__).resolve().parents[1] / "scripts"
sys.path.insert(0, str(SCRIPTS_DIR))

from nova_automation.project_github import Issue  # noqa: E402
from nova_automation.project_issue_metadata import (  # noqa: E402
    issue_form_field_names,
    legacy_metadata_reasons,
    parse_metadata,
    sync_labels_for_issue,
)


class FakeGitHubClient:
    """Small fake for REST helper tests."""

    def __init__(self, rest: list[Any]) -> None:
        self.rest_responses = list(rest)
        self.rest_calls: list[tuple[str, str, dict[str, Any] | None]] = []

    def rest(self, method: str, path: str, payload: dict[str, Any] | None = None) -> Any:
        self.rest_calls.append((method, path, payload))
        if not self.rest_responses:
            raise AssertionError(f"Unexpected REST call: {method} {path}")
        return self.rest_responses.pop(0)


def issue(body: str, labels: tuple[str, ...] = (), milestone: str | None = None) -> Issue:
    """Build a minimal issue fixture."""

    return Issue(
        number=1,
        node_id="I_test",
        title="Test issue",
        body=body,
        url="https://github.com/LucaPrevi0o/NovaLanguage/issues/1",
        state="open",
        labels=labels,
        milestone=milestone,
    )


def issue_payload(body: str, labels: tuple[str, ...] = ()) -> dict[str, Any]:
    """Build a minimal REST issue response."""

    return {
        "number": 1,
        "node_id": "I_test",
        "title": "Test issue",
        "body": body,
        "html_url": "https://github.com/LucaPrevi0o/NovaLanguage/issues/1",
        "state": "open",
        "labels": [{"name": label} for label in labels],
        "milestone": None,
    }


class ProjectIssueMetadataTest(unittest.TestCase):
    """Coverage for supported metadata parsing and legacy migration audit rules."""

    def test_issue_form_metadata_is_not_reported_as_legacy(self) -> None:
        body = """### Priority

1 - Important next step

### Size

M

### Suggested status

Ready
"""

        self.assertEqual(parse_metadata(body), {
            "Priority": "1 - Important next step",
            "Size": "M",
            "Suggested status": "Ready",
        })
        self.assertEqual(legacy_metadata_reasons(issue(body)), ())

    def test_legacy_project_metadata_block_is_reported(self) -> None:
        body = """## Project metadata

- Milestone: Nova MVP compiler
- Kind: test / feature
- Priority: P2
- Size: M
- Suggested status: Backlog
"""

        reasons = legacy_metadata_reasons(issue(body))

        self.assertEqual(parse_metadata(body), {})
        self.assertIn("legacy Project metadata block", reasons)
        self.assertIn("legacy Kind metadata", reasons)
        self.assertIn("Project field metadata in legacy block: Priority, Size, Suggested status", reasons)
        self.assertIn("native milestone missing; body metadata is still the roadmap source", reasons)
        self.assertIn("managed native label missing; body metadata is still the kind source", reasons)

    def test_native_metadata_satisfies_legacy_migration_dependencies(self) -> None:
        body = """## Project metadata

- Milestone: Nova MVP compiler
- Kind: test
"""

        reasons = legacy_metadata_reasons(issue(body, labels=("test",), milestone="Nova MVP compiler"))

        self.assertIn("legacy Project metadata block", reasons)
        self.assertIn("legacy Kind metadata", reasons)
        self.assertNotIn("native milestone missing; body metadata is still the roadmap source", reasons)
        self.assertNotIn("managed native label missing; body metadata is still the kind source", reasons)

    def test_legacy_schedule_fields_are_reported(self) -> None:
        body = """## Project metadata

- Expected start: 2026-07-01
- Expected deadline: 2026-07-31
"""

        reasons = legacy_metadata_reasons(issue(body))

        self.assertIn("schedule metadata in legacy block: Expected start, Expected deadline", reasons)

    def test_duplicated_native_issue_form_fields_are_reported(self) -> None:
        body = """### Milestone

Nova MVP compiler

### Labels

- [x] test

### Priority

P1
"""

        self.assertEqual(issue_form_field_names(body), ("Milestone", "Kind", "Priority"))
        self.assertEqual(parse_metadata(body), {"Priority": "P1"})

        reasons = legacy_metadata_reasons(issue(body))

        self.assertIn("duplicated native issue-form field(s): Milestone, Kind", reasons)

    def test_sync_labels_keeps_existing_managed_label_without_kind_metadata(self) -> None:
        body = """### Priority

1 - Important next step
"""
        client = FakeGitHubClient([issue_payload(body, labels=("docs",))])

        with patch("builtins.print") as print_mock:
            changed = sync_labels_for_issue(client, "LucaPrevi0o/NovaLanguage", 1, strict=False)  # type: ignore[arg-type]

        self.assertFalse(changed)
        self.assertEqual(len(client.rest_calls), 1)
        print_mock.assert_called_once_with("#1 has no Kind metadata; keeping existing managed labels: docs")


if __name__ == "__main__":
    unittest.main()
