#!/usr/bin/env python3
"""Compatibility wrapper for nova_automation.prepare_javadoc_site."""

from nova_automation.prepare_javadoc_site import *  # noqa: F401,F403
from nova_automation.prepare_javadoc_site import main as _main


if __name__ == "__main__":
    raise SystemExit(_main())
