#!/usr/bin/env python3
"""Compatibility wrapper for nova_automation.mirror_docs_to_wiki."""

from nova_automation.mirror_docs_to_wiki import *  # noqa: F401,F403
from nova_automation.mirror_docs_to_wiki import main as _main


if __name__ == "__main__":
    raise SystemExit(_main())
