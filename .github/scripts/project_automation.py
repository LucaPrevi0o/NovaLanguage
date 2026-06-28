#!/usr/bin/env python3
"""Compatibility wrapper for nova_automation.project_automation."""

from nova_automation.project_automation import *  # noqa: F401,F403
from nova_automation.project_automation import main as _main


if __name__ == "__main__":
    raise SystemExit(_main())
