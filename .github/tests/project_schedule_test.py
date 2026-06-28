#!/usr/bin/env python3
"""Unit tests for Nova's Project schedule helper."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parents[1] / "scripts"
sys.path.insert(0, str(SCRIPTS_DIR))

from nova_automation.project_schedule import parse_schedule  # noqa: E402


class ProjectScheduleMetadataTest(unittest.TestCase):
    """Coverage for current schedule metadata parsing."""

    def test_issue_form_schedule_fields_are_parsed(self) -> None:
        body = """### Expected start

2026-07-01

### Expected deadline

2026-07-31
"""

        self.assertEqual(parse_schedule(body), {
            "Expected start": "2026-07-01",
            "Expected deadline": "2026-07-31",
        })

    def test_legacy_project_metadata_schedule_is_ignored(self) -> None:
        body = """## Project metadata

- Expected start: 2026-07-01
- Expected deadline: 2026-07-31
"""

        self.assertEqual(parse_schedule(body), {})


if __name__ == "__main__":
    unittest.main()
