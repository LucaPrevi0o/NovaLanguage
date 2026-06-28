#!/usr/bin/env python3
"""Compatibility wrapper for nova_automation.docs.wiki."""

from nova_automation.docs.wiki import *  # noqa: F401,F403
from nova_automation.docs.wiki import main as _main


if __name__ == "__main__":
    raise SystemExit(_main())
