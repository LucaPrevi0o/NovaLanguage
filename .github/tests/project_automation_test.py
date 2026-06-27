#!/usr/bin/env python3
"""Unit tests for Nova's GitHub automation helpers."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

SCRIPTS_DIR = Path(__file__).resolve().parents[1] / "scripts"
sys.path.insert(0, str(SCRIPTS_DIR))

import project_automation  # noqa: E402
from project_automation import (  # noqa: E402
    issue_form_field_names,
    legacy_metadata_reasons,
    parse_metadata,
)
from project_github import (  # noqa: E402
    Issue,
    ProjectAutomationError,
    PullRequest,
)


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


class ProjectAutomationMetadataTest(unittest.TestCase):
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


class ProjectAutomationPullRequestStatusTest(unittest.TestCase):
    """Coverage for syncing referenced issue status from pull request state."""

    def test_sync_pr_status_skips_pull_request_references(self) -> None:
        pull_request = PullRequest(
            number=66,
            title="refactor: extract automation GitHub helpers",
            body="Refs #63.\n\nThis PR starts the post-#65 helper split.",
            url="https://github.com/LucaPrevi0o/NovaLanguage/pull/66",
            draft=False,
            merged=False,
        )
        status_calls: list[tuple[str, int, str]] = []

        def fake_set_issue_status(client: object, repository: str, issue_number: int, status: str) -> None:
            if issue_number == 65:
                raise ProjectAutomationError("#65 is a pull request, not an issue")
            status_calls.append((repository, issue_number, status))

        with (
            patch("project_automation.GitHubClient", return_value=object()),
            patch("project_automation.token_from_environment", return_value="token"),
            patch("project_automation.get_pull_request", return_value=pull_request),
            patch("project_automation.set_issue_status", side_effect=fake_set_issue_status),
            patch("builtins.print") as print_mock,
        ):
            result = project_automation.sync_pr_status_command(
                SimpleNamespace(repo="LucaPrevi0o/NovaLanguage", pr_number=66)
            )

        self.assertEqual(result, 0)
        self.assertEqual(status_calls, [("LucaPrevi0o/NovaLanguage", 63, "In Review")])
        print_mock.assert_any_call("::warning::Skipping #65 because it is a pull request, not an issue")

    def test_sync_pr_status_keeps_other_errors_fatal(self) -> None:
        pull_request = PullRequest(
            number=66,
            title="refactor: extract automation GitHub helpers",
            body="Refs #63.",
            url="https://github.com/LucaPrevi0o/NovaLanguage/pull/66",
            draft=False,
            merged=False,
        )

        with (
            patch("project_automation.GitHubClient", return_value=object()),
            patch("project_automation.token_from_environment", return_value="token"),
            patch("project_automation.get_pull_request", return_value=pull_request),
            patch("project_automation.set_issue_status", side_effect=ProjectAutomationError("GitHub API 500")),
        ):
            with self.assertRaisesRegex(ProjectAutomationError, "GitHub API 500"):
                project_automation.sync_pr_status_command(
                    SimpleNamespace(repo="LucaPrevi0o/NovaLanguage", pr_number=66)
                )


if __name__ == "__main__":
    unittest.main()
