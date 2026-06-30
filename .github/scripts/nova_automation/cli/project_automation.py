#!/usr/bin/env python3
"""GitHub Project automation CLI for NovaLanguage."""

from __future__ import annotations

import argparse
import os
import sys

from ..github import (
    GitHubClient,
    ProjectAutomationError,
    get_issue,
    list_closed_issues,
    list_open_issues,
    token_from_environment,
)
from ..issues.labels import sync_labels_for_issue
from ..project.board import remove_project_field, sync_issue_archive
from ..project.issue_metadata import (
    LegacyMetadataFinding,
    legacy_metadata_issue_numbers,
    legacy_metadata_reasons,
    sync_issue,
)
from ..project.metadata import LEGACY_KIND_FIELD, LEGACY_PHASE_FIELD
from ..pull_requests.status import sync_pr_status_command
from ..roadmap.drift import check_plan_drift_command


def audit_legacy_metadata_command(args: argparse.Namespace) -> int:
    """Run the audit-legacy-metadata subcommand."""

    client = GitHubClient(token_from_environment())
    issue_numbers = legacy_metadata_issue_numbers(client, args.repo, args)

    findings: list[LegacyMetadataFinding] = []
    failures = 0
    for number in issue_numbers:
        try:
            issue = get_issue(client, args.repo, number)
        except ProjectAutomationError as error:
            failures += 1
            print(f"::error::Failed to inspect #{number}: {error}")
            continue

        reasons = legacy_metadata_reasons(issue)
        if reasons:
            findings.append(LegacyMetadataFinding(issue, reasons))

    for finding in findings:
        reason_text = "; ".join(finding.reasons)
        print(f"::warning::{finding.issue.url} uses legacy metadata: {reason_text}")

    if findings:
        print(f"Found {len(findings)} issue(s) with legacy metadata migration markers.")
    else:
        print("No legacy metadata migration markers found.")

    if failures:
        return 1
    if findings and args.fail_on_findings:
        return 1
    return 0


def sync_issue_command(args: argparse.Namespace) -> int:
    """Run the sync-issue subcommand."""

    client = GitHubClient(token_from_environment())

    issue_numbers: list[int]
    if args.all_open:
        issue_numbers = list_open_issues(client, args.repo)
    elif args.issue_number is not None:
        issue_numbers = [args.issue_number]
    else:
        raise ProjectAutomationError("Provide --issue-number or --all-open")

    failures = 0
    for number in issue_numbers:
        try:
            sync_issue(client, args.repo, number, args.strict)
        except ProjectAutomationError as error:
            failures += 1
            print(f"::error::Failed to sync #{number}: {error}")
            if args.strict:
                break

    return 1 if failures else 0


def sync_issue_archive_command(args: argparse.Namespace) -> int:
    """Run the sync-issue-archive subcommand."""

    client = GitHubClient(token_from_environment())

    issue_numbers: list[int]
    selected_modes = sum(1 for selected in (args.issue_number is not None, args.all_open, args.all_closed) if selected)
    if selected_modes != 1:
        raise ProjectAutomationError("Provide exactly one of --issue-number, --all-open, or --all-closed")
    if args.issue_number is not None:
        issue_numbers = [args.issue_number]
    elif args.all_open:
        issue_numbers = list_open_issues(client, args.repo)
    else:
        issue_numbers = list_closed_issues(client, args.repo)

    failures = 0
    for number in issue_numbers:
        try:
            sync_issue_archive(client, args.repo, number)
        except ProjectAutomationError as error:
            failures += 1
            print(f"::error::Failed to sync archive state for #{number}: {error}")

    return 1 if failures else 0


def sync_labels_command(args: argparse.Namespace) -> int:
    """Run the sync-labels subcommand."""

    client = GitHubClient(token_from_environment())

    issue_numbers: list[int]
    if args.all_open:
        issue_numbers = list_open_issues(client, args.repo)
    elif args.issue_number is not None:
        issue_numbers = [args.issue_number]
    else:
        raise ProjectAutomationError("Provide --issue-number or --all-open")

    failures = 0
    for number in issue_numbers:
        try:
            sync_labels_for_issue(client, args.repo, number, args.strict)
        except ProjectAutomationError as error:
            failures += 1
            print(f"::error::Failed to sync labels for #{number}: {error}")
            if args.strict:
                break

    return 1 if failures else 0


def remove_legacy_phase_field_command(args: argparse.Namespace) -> int:
    """Run the remove-legacy-phase-field subcommand."""

    if not args.confirm:
        raise ProjectAutomationError(
            "Refusing to remove the legacy Project Phase field without --confirm. "
            "Run this only when the Project should not expose the duplicate Phase field."
        )

    client = GitHubClient(token_from_environment())
    remove_project_field(client, LEGACY_PHASE_FIELD)
    return 0


def remove_legacy_kind_field_command(args: argparse.Namespace) -> int:
    """Run the remove-legacy-kind-field subcommand."""

    if not args.confirm:
        raise ProjectAutomationError(
            "Refusing to remove the legacy Project Kind field without --confirm. "
            "Run this only when issue labels are the source of truth for kind metadata."
        )

    client = GitHubClient(token_from_environment())
    remove_project_field(client, LEGACY_KIND_FIELD)
    return 0


def build_parser() -> argparse.ArgumentParser:
    """Build the command-line parser."""

    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    sync_parser = subparsers.add_parser("sync-issue", help="Sync issue metadata into milestones and Project fields")
    sync_parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY"), help="Repository in owner/name form")
    sync_parser.add_argument("--issue-number", type=int, help="Issue number to sync")
    sync_parser.add_argument("--all-open", action="store_true", help="Sync all open issues")
    sync_parser.add_argument("--strict", action="store_true", help="Fail on missing or invalid issue metadata")
    sync_parser.set_defaults(func=sync_issue_command)

    pr_parser = subparsers.add_parser("sync-pr-status", help="Sync Project issue status from pull request state")
    pr_parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY"), help="Repository in owner/name form")
    pr_parser.add_argument("--pr-number", type=int, required=True, help="Pull request number to inspect")
    pr_parser.set_defaults(func=sync_pr_status_command)

    labels_parser = subparsers.add_parser("sync-labels", help="Sync managed issue labels from metadata")
    labels_parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY"), help="Repository in owner/name form")
    labels_parser.add_argument("--issue-number", type=int, help="Issue number to sync")
    labels_parser.add_argument("--all-open", action="store_true", help="Sync labels for all open issues")
    labels_parser.add_argument("--strict", action="store_true", help="Fail on missing or invalid issue metadata")
    labels_parser.set_defaults(func=sync_labels_command)

    audit_parser = subparsers.add_parser(
        "audit-legacy-metadata",
        help="Report issues that still contain legacy body metadata",
    )
    audit_parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY"), help="Repository in owner/name form")
    audit_parser.add_argument("--issue-number", type=int, help="Issue number to inspect")
    audit_parser.add_argument("--all-open", action="store_true", help="Inspect all open issues")
    audit_parser.add_argument("--all-closed", action="store_true", help="Inspect all closed issues")
    audit_parser.add_argument("--all", action="store_true", help="Inspect all issues")
    audit_parser.add_argument(
        "--fail-on-findings",
        action="store_true",
        help="Exit non-zero when legacy metadata is found",
    )
    audit_parser.set_defaults(func=audit_legacy_metadata_command)

    archive_parser = subparsers.add_parser(
        "sync-issue-archive",
        help="Archive closed Done issues and unarchive open issues in the Project",
    )
    archive_parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY"), help="Repository in owner/name form")
    archive_parser.add_argument("--issue-number", type=int, help="Issue number to sync")
    archive_parser.add_argument("--all-open", action="store_true", help="Sync archive state for all open issues")
    archive_parser.add_argument("--all-closed", action="store_true", help="Sync archive state for all closed issues")
    archive_parser.set_defaults(func=sync_issue_archive_command)

    drift_parser = subparsers.add_parser("check-plan-drift", help="Check PLAN.md and Project state alignment")
    drift_parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY"), help="Repository in owner/name form")
    drift_parser.add_argument("--plan", default="PLAN.md", help="Path to PLAN.md")
    drift_parser.add_argument("--readme", default="README.md", help="Path to README.md")
    drift_parser.add_argument("--fail-on-warning", action="store_true", help="Exit non-zero when warnings are found")
    drift_parser.set_defaults(func=check_plan_drift_command)

    cleanup_parser = subparsers.add_parser(
        "remove-legacy-phase-field",
        help="Remove the legacy custom Project Phase field after milestone migration",
    )
    cleanup_parser.add_argument(
        "--confirm",
        action="store_true",
        help="Confirm that the duplicate Project Phase field should be removed",
    )
    cleanup_parser.set_defaults(func=remove_legacy_phase_field_command)

    kind_cleanup_parser = subparsers.add_parser(
        "remove-legacy-kind-field",
        help="Remove the legacy custom Project Kind field after label migration",
    )
    kind_cleanup_parser.add_argument(
        "--confirm",
        action="store_true",
        help="Confirm that issue labels should be the only Kind projection",
    )
    kind_cleanup_parser.set_defaults(func=remove_legacy_kind_field_command)

    return parser


def main() -> int:
    """Run the selected automation command."""

    parser = build_parser()
    args = parser.parse_args()
    if hasattr(args, "repo") and not args.repo:
        parser.error("--repo is required when GITHUB_REPOSITORY is not set")
    try:
        return args.func(args)
    except ProjectAutomationError as error:
        print(f"::error::{error}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
