#!/usr/bin/env python3
"""Compatibility wrapper for nova_automation.issues.forms."""

from nova_automation.issues.forms import *  # noqa: F401,F403
from nova_automation.issues.forms import main as _main


if __name__ == "__main__":
    raise SystemExit(_main())
