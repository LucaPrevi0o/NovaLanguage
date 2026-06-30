"""Sync Project issue status from pull request state."""

from __future__ import annotations

import argparse

from ..github import GitHubClient, ProjectAutomationError, get_pull_request, token_from_environment
from ..project.board import set_issue_status, sync_issue_archive
from ..project.metadata import DONE_STATUS
from .references import closing_issue_reference_numbers, issue_reference_numbers


def is_pull_request_reference_error(error: ProjectAutomationError, issue_number: int) -> bool:
    """Return whether an issue lookup failed because the reference is a pull request."""

    return str(error) == f"#{issue_number} is a pull request, not an issue"


def sync_pr_status_command(args: argparse.Namespace) -> int:
    """Run the sync-pr-status subcommand."""

    client = GitHubClient(token_from_environment())
    pull_request = get_pull_request(client, args.repo, args.pr_number)

    if pull_request.draft:
        print(f"PR #{pull_request.number} is a draft; not changing issue status")
        return 0

    if pull_request.merged:
        targets = closing_issue_reference_numbers(args.repo, pull_request.body)
        if not targets:
            print(f"::warning::PR #{pull_request.number} was merged but has no closing issue references")
            return 0
        target_status = "Done"
    else:
        targets = issue_reference_numbers(args.repo, pull_request.body)
        if not targets:
            print(f"::warning::PR #{pull_request.number} has no issue references")
            return 0
        target_status = "In Review"

    for issue_number in sorted(targets):
        try:
            set_issue_status(client, args.repo, issue_number, target_status)
        except ProjectAutomationError as error:
            if is_pull_request_reference_error(error, issue_number):
                print(f"::warning::Skipping #{issue_number} because it is a pull request, not an issue")
                continue
            raise
        if target_status == DONE_STATUS:
            sync_issue_archive(client, args.repo, issue_number)
    return 0
