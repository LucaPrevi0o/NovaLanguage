#!/usr/bin/env python3
"""Align pull request tracking metadata with referenced issues."""

from __future__ import annotations

import argparse
import os
import sys
from dataclasses import dataclass

from .project_automation import (
    issue_reference_numbers,
)
from .project_board import (
    add_content_to_project,
    get_project,
    project_item_id_for_content,
    project_issue_item,
    set_single_select_value,
)
from .project_github import (
    GitHubClient,
    Project,
    ProjectAutomationError,
    get_issue,
    token_from_environment,
)
from .project_metadata import MANAGED_KIND_LABELS, PRIORITY_FIELD, SIZE_FIELD, STATUS_FIELD

PROJECT_STATUS_FOR_READY_PR = os.environ.get("PROJECT_PR_READY_STATUS", "In Review")
COPIED_PROJECT_FIELDS = (PRIORITY_FIELD, SIZE_FIELD)


class PullRequestMetadataError(RuntimeError):
    """Raised when pull request metadata cannot be aligned safely."""


@dataclass(frozen=True)
class ReferencedIssue:
    """Issue metadata copied onto a pull request."""

    number: int
    node_id: str
    title: str
    body: str
    labels: tuple[str, ...]
    milestone_title: str | None
    milestone_number: int | None


@dataclass(frozen=True)
class PullRequestIssue:
    """Pull request plus its issue-like metadata."""

    number: int
    node_id: str
    title: str
    body: str
    draft: bool
    labels: tuple[str, ...]
    milestone_title: str | None
    milestone_number: int | None


def referenced_issue_from_payload(payload: dict) -> ReferencedIssue:
    """Build copied issue metadata from a REST issue payload."""

    milestone = payload.get("milestone")
    return ReferencedIssue(
        number=payload["number"],
        node_id=payload["node_id"],
        title=payload["title"],
        body=payload.get("body") or "",
        labels=tuple(label["name"] for label in payload.get("labels", [])),
        milestone_title=milestone["title"] if milestone else None,
        milestone_number=milestone["number"] if milestone else None,
    )


def get_referenced_issue(client: GitHubClient, repository: str, issue_number: int) -> ReferencedIssue:
    """Fetch one referenced issue and reject pull requests."""

    try:
        payload = client.rest("GET", f"/repos/{repository}/issues/{issue_number}")
    except ProjectAutomationError as error:
        raise PullRequestMetadataError(str(error)) from error
    if "pull_request" in payload:
        raise PullRequestMetadataError(f"#{issue_number} is a pull request, not a source issue")
    return referenced_issue_from_payload(payload)


def get_pull_request_issue(client: GitHubClient, repository: str, pr_number: int) -> PullRequestIssue:
    """Fetch pull request metadata from pull and issue REST endpoints."""

    try:
        pull_payload = client.rest("GET", f"/repos/{repository}/pulls/{pr_number}")
        issue_payload = client.rest("GET", f"/repos/{repository}/issues/{pr_number}")
    except ProjectAutomationError as error:
        raise PullRequestMetadataError(str(error)) from error
    milestone = issue_payload.get("milestone")
    return PullRequestIssue(
        number=pull_payload["number"],
        node_id=pull_payload["node_id"],
        title=pull_payload["title"],
        body=pull_payload.get("body") or "",
        draft=pull_payload.get("draft", False),
        labels=tuple(label["name"] for label in issue_payload.get("labels", [])),
        milestone_title=milestone["title"] if milestone else None,
        milestone_number=milestone["number"] if milestone else None,
    )


def copied_labels(issues: list[ReferencedIssue]) -> tuple[str, ...]:
    """Return managed issue labels to add to a pull request."""

    managed = {entry[0] for entry in MANAGED_KIND_LABELS.values()}
    labels: list[str] = []
    seen: set[str] = set()
    for issue in issues:
        for label in issue.labels:
            if label in managed and label not in seen:
                labels.append(label)
                seen.add(label)
    return tuple(labels)


def add_missing_labels(client: GitHubClient, repository: str, pull_request: PullRequestIssue, labels: tuple[str, ...]) -> bool:
    """Add managed labels from referenced issues without removing manual PR labels."""

    missing = sorted(set(labels) - set(pull_request.labels))
    if not missing:
        print(f"PR #{pull_request.number} already has referenced issue labels")
        return False
    try:
        client.rest("POST", f"/repos/{repository}/issues/{pull_request.number}/labels", {"labels": missing})
    except ProjectAutomationError as error:
        raise PullRequestMetadataError(str(error)) from error
    print(f"Added labels to PR #{pull_request.number}: {', '.join(missing)}")
    return True


def shared_milestone(issues: list[ReferencedIssue]) -> tuple[str, int] | None:
    """Return one shared source milestone, or None when absent/conflicting."""

    milestones = {
        (issue.milestone_title, issue.milestone_number)
        for issue in issues
        if issue.milestone_title and issue.milestone_number is not None
    }
    if not milestones:
        print("::notice::Referenced issues have no milestone; leaving PR milestone unchanged")
        return None
    if len(milestones) > 1:
        labels = ", ".join(title or str(number) for title, number in sorted(milestones))
        print(f"::warning::Referenced issues use different milestones ({labels}); leaving PR milestone unchanged")
        return None
    title, number = milestones.pop()
    return title or str(number), number


def set_pull_request_milestone(client: GitHubClient, repository: str, pull_request: PullRequestIssue, issues: list[ReferencedIssue]) -> bool:
    """Copy a shared referenced issue milestone to the pull request."""

    milestone = shared_milestone(issues)
    if milestone is None:
        return False
    title, number = milestone
    if pull_request.milestone_number == number:
        print(f"PR #{pull_request.number} already has milestone '{title}'")
        return False
    try:
        client.rest("PATCH", f"/repos/{repository}/issues/{pull_request.number}", {"milestone": number})
    except ProjectAutomationError as error:
        raise PullRequestMetadataError(str(error)) from error
    print(f"Set PR #{pull_request.number} milestone to '{title}'")
    return True


def add_pr_to_project(client: GitHubClient, project: Project, pull_request: PullRequestIssue) -> str:
    """Add a pull request to the Project and return its item ID."""

    try:
        existing = project_item_id_for_content(client, project, pull_request.node_id)
        if existing:
            return existing
        item_id = add_content_to_project(client, project, pull_request.node_id)
    except ProjectAutomationError as error:
        raise PullRequestMetadataError(str(error)) from error
    print(f"Added PR #{pull_request.number} to Project")
    return item_id


def source_project_fields(client: GitHubClient, project: Project, repository: str, issues: list[ReferencedIssue]) -> dict[str, str]:
    """Return Project fields that can be copied from referenced issues."""

    values: dict[str, set[str]] = {field: set() for field in COPIED_PROJECT_FIELDS}
    for source in issues:
        try:
            issue = get_issue(client, repository, source.number)
            item = project_issue_item(client, project, issue)
        except ProjectAutomationError as error:
            raise PullRequestMetadataError(str(error)) from error
        if item is None:
            print(f"::notice::Referenced issue #{source.number} is not in the Project; skipping Project field copy")
            continue
        for field_name in COPIED_PROJECT_FIELDS:
            value = item.fields.get(field_name)
            if value:
                values[field_name].add(value)

    copied: dict[str, str] = {}
    for field_name, field_values in values.items():
        if len(field_values) == 1:
            copied[field_name] = next(iter(field_values))
        elif len(field_values) > 1:
            joined = ", ".join(sorted(field_values))
            print(f"::warning::Referenced issues disagree on {field_name} ({joined}); not copying it")
    return copied


def set_project_select_field(client: GitHubClient, project: Project, item_id: str, field_name: str, option_name: str) -> bool:
    """Set one Project single-select field when the field and option exist."""

    field = project.fields.get(field_name)
    if field is None:
        print(f"::warning::Project field '{field_name}' was not found; skipping")
        return False
    option_id = field.options.get(option_name)
    if option_id is None:
        print(f"::warning::Project field '{field_name}' has no option '{option_name}'; skipping")
        return False
    try:
        set_single_select_value(client, project, item_id, field, option_id)
    except ProjectAutomationError as error:
        raise PullRequestMetadataError(str(error)) from error
    print(f"Set PR Project field {field_name} = {option_name}")
    return True


def align_project_item(client: GitHubClient, repository: str, pull_request: PullRequestIssue, issues: list[ReferencedIssue]) -> bool:
    """Add the PR to the Project and copy selected Project fields from referenced issues."""

    try:
        project = get_project(client)
    except ProjectAutomationError as error:
        raise PullRequestMetadataError(str(error)) from error
    item_id = add_pr_to_project(client, project, pull_request)

    changed = False
    for field_name, value in source_project_fields(client, project, repository, issues).items():
        changed = set_project_select_field(client, project, item_id, field_name, value) or changed
    if pull_request.draft:
        print(f"PR #{pull_request.number} is a draft; not setting PR Project status")
        return changed
    return set_project_select_field(client, project, item_id, STATUS_FIELD, PROJECT_STATUS_FOR_READY_PR) or changed


def referenced_issues(client: GitHubClient, repository: str, pull_request: PullRequestIssue) -> list[ReferencedIssue]:
    """Fetch same-repository issues referenced by the pull request body."""

    issues: list[ReferencedIssue] = []
    for number in sorted(issue_reference_numbers(repository, pull_request.body)):
        if number == pull_request.number:
            continue
        try:
            issues.append(get_referenced_issue(client, repository, number))
        except PullRequestMetadataError as error:
            print(f"::warning::{error}")
    return issues


def align_pull_request_metadata(client: GitHubClient, repository: str, pr_number: int) -> bool:
    """Align one pull request with its referenced issues."""

    pull_request = get_pull_request_issue(client, repository, pr_number)
    issues = referenced_issues(client, repository, pull_request)
    if not issues:
        print(f"::warning::PR #{pull_request.number} does not reference any same-repository source issues")
        return False
    issue_labels = ", ".join(f"#{issue.number}" for issue in issues)
    print(f"Aligning PR #{pull_request.number} with referenced issues: {issue_labels}")
    changed = add_missing_labels(client, repository, pull_request, copied_labels(issues))
    changed = set_pull_request_milestone(client, repository, pull_request, issues) or changed
    changed = align_project_item(client, repository, pull_request, issues) or changed
    return changed


def build_parser() -> argparse.ArgumentParser:
    """Build the command-line parser."""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY"), help="Repository in owner/name form")
    parser.add_argument("--pr-number", type=int, required=True, help="Pull request number to align")
    return parser


def main() -> int:
    """Run the pull request metadata alignment command."""

    parser = build_parser()
    args = parser.parse_args()
    if not args.repo:
        parser.error("--repo is required when GITHUB_REPOSITORY is not set")
    client = GitHubClient(token_from_environment())
    try:
        align_pull_request_metadata(client, args.repo, args.pr_number)
    except PullRequestMetadataError as error:
        print(f"::error::{error}")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
