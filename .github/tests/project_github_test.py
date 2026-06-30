#!/usr/bin/env python3
"""Unit tests for shared GitHub automation helpers."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parents[1] / "scripts"
sys.path.insert(0, str(SCRIPTS_DIR))

from nova_automation.github import (  # noqa: E402
    GitHubClient,
    Issue,
    ProjectAutomationError,
    repository_parts,
)


class ProjectGitHubHelperTest(unittest.TestCase):
    """Coverage for shared GitHub helper behavior that does not require network access."""

    def test_repository_parts_splits_owner_and_name(self) -> None:
        self.assertEqual(repository_parts("LucaPrevi0o/NovaLanguage"), ("LucaPrevi0o", "NovaLanguage"))

    def test_repository_parts_rejects_unqualified_name(self) -> None:
        with self.assertRaises(ProjectAutomationError):
            repository_parts("NovaLanguage")

    def test_client_requires_token(self) -> None:
        with self.assertRaises(ProjectAutomationError):
            GitHubClient("")

    def test_issue_model_defaults_optional_native_metadata(self) -> None:
        issue = Issue(
            number=1,
            node_id="I_test",
            title="Test",
            body="",
            url="https://github.com/LucaPrevi0o/NovaLanguage/issues/1",
            state="open",
        )

        self.assertEqual(issue.labels, ())
        self.assertIsNone(issue.milestone)


if __name__ == "__main__":
    unittest.main()
