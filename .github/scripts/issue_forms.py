#!/usr/bin/env python3
"""Compatibility wrapper for nova_automation.issue_forms."""

from nova_automation.issue_forms import *  # noqa: F401,F403
from nova_automation.issue_forms import main as _main


if __name__ == "__main__":
    raise SystemExit(_main())
