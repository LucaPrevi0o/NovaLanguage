#!/usr/bin/env python3
"""Compatibility wrapper for nova_automation.pull_requests.metadata_alignment."""

from nova_automation.pull_requests.metadata_alignment import *  # noqa: F401,F403
from nova_automation.pull_requests.metadata_alignment import main as _main


if __name__ == "__main__":
    raise SystemExit(_main())
