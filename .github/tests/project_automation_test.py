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

import nova_automation.project_automation as project_automation  # noqa: E402
from nova_automation.project_github import (  # noqa: E402
    ProjectAutomationError,
    PullRequest,
)


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
            patch("nova_automation.project_automation.GitHubClient", return_value=object()),
            patch("nova_automation.project_automation.token_from_environment", return_value="token"),
            patch("nova_automation.project_automation.get_pull_request", return_value=pull_request),
            patch("nova_automation.project_automation.set_issue_status", side_effect=fake_set_issue_status),
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
            patch("nova_automation.project_automation.GitHubClient", return_value=object()),
            patch("nova_automation.project_automation.token_from_environment", return_value="token"),
            patch("nova_automation.project_automation.get_pull_request", return_value=pull_request),
            patch("nova_automation.project_automation.set_issue_status", side_effect=ProjectAutomationError("GitHub API 500")),
        ):
            with self.assertRaisesRegex(ProjectAutomationError, "GitHub API 500"):
                project_automation.sync_pr_status_command(
                    SimpleNamespace(repo="LucaPrevi0o/NovaLanguage", pr_number=66)
                )


if __name__ == "__main__":
    unittest.main()
