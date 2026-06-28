#!/usr/bin/env python3
"""Compatibility wrapper for nova_automation.cli.automation_health."""

from nova_automation.cli.automation_health import *  # noqa: F401,F403
from nova_automation.cli.automation_health import main as _main


if __name__ == "__main__":
    raise SystemExit(_main())
