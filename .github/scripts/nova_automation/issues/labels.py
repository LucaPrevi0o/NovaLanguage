"""Managed issue-label synchronization helpers."""

from __future__ import annotations

from urllib.parse import quote

from ..github import GitHubClient, ProjectAutomationError, get_issue
from ..project.issue_metadata import kind_labels_from_metadata, parse_metadata
from ..project.metadata import MANAGED_KIND_LABELS


def ensure_label(client: GitHubClient, repository: str, label: str, color: str, description: str) -> None:
    """Ensure a repository label exists."""

    encoded_label = quote(label, safe="")
    try:
        client.rest("GET", f"/repos/{repository}/labels/{encoded_label}")
        return
    except ProjectAutomationError as error:
        if "GitHub API 404" not in str(error):
            raise

    client.rest(
        "POST",
        f"/repos/{repository}/labels",
        {
            "name": label,
            "color": color,
            "description": description,
        },
    )
    print(f"Created label '{label}'")


def add_issue_label(client: GitHubClient, repository: str, issue_number: int, label: str) -> None:
    """Add one label to an issue."""

    client.rest("POST", f"/repos/{repository}/issues/{issue_number}/labels", {"labels": [label]})


def remove_issue_label(client: GitHubClient, repository: str, issue_number: int, label: str) -> None:
    """Remove one label from an issue."""

    encoded_label = quote(label, safe="")
    try:
        client.rest("DELETE", f"/repos/{repository}/issues/{issue_number}/labels/{encoded_label}")
    except ProjectAutomationError as error:
        if "GitHub API 404" not in str(error):
            raise


def sync_labels_for_issue(client: GitHubClient, repository: str, issue_number: int, strict: bool) -> bool:
    """Sync one issue's managed kind labels from its metadata."""

    issue = get_issue(client, repository, issue_number)
    metadata = parse_metadata(issue.body)
    if not metadata:
        message = f"#{issue.number} has no current issue-form Project metadata; skipping label sync"
        if strict:
            raise ProjectAutomationError(message)
        print(f"::warning::{message}")
        return False

    managed_labels = {entry[0] for entry in MANAGED_KIND_LABELS.values()}
    target_labels, unknown_kinds = kind_labels_from_metadata(metadata)
    if not target_labels and not unknown_kinds:
        existing_labels = sorted(set(issue.labels) & managed_labels)
        if existing_labels:
            labels = ", ".join(existing_labels)
            print(f"#{issue.number} has no Kind metadata; keeping existing managed labels: {labels}")
            return False
        message = f"#{issue.number} has no Kind metadata; skipping label sync"
        if strict:
            raise ProjectAutomationError(message)
        print(f"::warning::{message}")
        return False

    if unknown_kinds:
        unknown_list = ", ".join(unknown_kinds)
        message = f"Kind metadata contains unmanaged label values: {unknown_list}"
        if strict:
            raise ProjectAutomationError(message)
        print(f"::warning::{message}")

    target_label_set = set(target_labels)
    if not target_label_set:
        message = f"#{issue.number} has no managed Kind labels to sync"
        if strict:
            raise ProjectAutomationError(message)
        print(f"::warning::{message}")
        return False

    for target_label in target_labels:
        label_name, color, description = MANAGED_KIND_LABELS[target_label]
        ensure_label(client, repository, label_name, color, description)

    changed = False
    current_labels = set(issue.labels)
    for stale_label in sorted((current_labels & managed_labels) - target_label_set):
        remove_issue_label(client, repository, issue.number, stale_label)
        print(f"Removed stale managed label '{stale_label}' from #{issue.number}")
        changed = True

    for label_name in sorted(target_label_set - current_labels):
        add_issue_label(client, repository, issue.number, label_name)
        print(f"Added label '{label_name}' to #{issue.number}")
        changed = True

    if not changed:
        labels = ", ".join(sorted(target_label_set))
        print(f"#{issue.number} already has managed labels: {labels}")
    return changed
