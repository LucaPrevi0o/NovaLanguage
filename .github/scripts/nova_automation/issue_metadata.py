#!/usr/bin/env python3
"""Check native GitHub issue labels and milestones against managed metadata."""

from __future__ import annotations

import argparse
import os

from .project_metadata import MANAGED_MILESTONES, managed_label_names


class IssueMetadataError(RuntimeError):
    """Raised when native issue metadata is incomplete or unmanaged."""


def label_values(raw_labels: str) -> tuple[str, ...]:
    """Parse comma-separated label names passed by GitHub Actions."""

    return tuple(label.strip() for label in raw_labels.split(",") if label.strip())


def issue_label(issue_number: str) -> str:
    """Return a display label for one issue number."""

    return f"Issue #{issue_number}" if issue_number else "Issue"


def check_issue_metadata(issue_number: str, labels: tuple[str, ...], milestone: str) -> None:
    """Validate native issue labels and milestone names."""

    managed_labels = set(managed_label_names())
    if not managed_labels.intersection(labels):
        expected = ", ".join(managed_label_names())
        actual = ", ".join(labels) if labels else "none"
        raise IssueMetadataError(
            f"{issue_label(issue_number)} has no managed label. "
            f"Found: {actual}. Add at least one of: {expected}."
        )
    print(f"Managed issue label found: {', '.join(labels)}")

    if not milestone:
        expected = ", ".join(MANAGED_MILESTONES)
        raise IssueMetadataError(
            f"{issue_label(issue_number)} has no milestone. Select one managed roadmap milestone: {expected}."
        )

    if milestone not in MANAGED_MILESTONES:
        expected = ", ".join(MANAGED_MILESTONES)
        raise IssueMetadataError(
            f"{issue_label(issue_number)} uses unmanaged milestone '{milestone}'. "
            f"Expected one of: {expected}."
        )
    print(f"Managed issue milestone found: {milestone}")


def build_parser() -> argparse.ArgumentParser:
    """Build the command-line parser."""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--issue-number", default=os.environ.get("ISSUE_NUMBER", ""), help="Issue number for messages")
    parser.add_argument("--labels", default=os.environ.get("ISSUE_LABELS", ""), help="Comma-separated issue labels")
    parser.add_argument("--milestone", default=os.environ.get("ISSUE_MILESTONE", ""), help="Native issue milestone title")
    return parser


def main() -> int:
    """Run the issue metadata check."""

    args = build_parser().parse_args()
    try:
        check_issue_metadata(args.issue_number, label_values(args.labels), args.milestone)
    except IssueMetadataError as error:
        print(f"::error::{error}")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
