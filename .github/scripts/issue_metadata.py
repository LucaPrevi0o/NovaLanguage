#!/usr/bin/env python3
"""Compatibility wrapper for nova_automation.issue_metadata."""

from nova_automation.issue_metadata import *  # noqa: F401,F403
from nova_automation.issue_metadata import main as _main


if __name__ == "__main__":
    raise SystemExit(_main())
