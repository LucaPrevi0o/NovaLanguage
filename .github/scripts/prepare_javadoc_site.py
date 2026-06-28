#!/usr/bin/env python3
"""Compatibility wrapper for nova_automation.docs.pages."""

from nova_automation.docs.pages import *  # noqa: F401,F403
from nova_automation.docs.pages import main as _main


if __name__ == "__main__":
    raise SystemExit(_main())
