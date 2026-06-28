"""Repository-level GitHub API helpers for Nova automation."""

from __future__ import annotations

from .client import GitHubClient
from .models import Issue, ProjectAutomationError, PullRequest


def get_issue(client: GitHubClient, repository: str, number: int) -> Issue:
    """Fetch one issue through the REST API."""

    payload = client.rest("GET", f"/repos/{repository}/issues/{number}")
    if "pull_request" in payload:
        raise ProjectAutomationError(f"#{number} is a pull request, not an issue")
    return Issue(
        number=payload["number"],
        node_id=payload["node_id"],
        title=payload["title"],
        body=payload.get("body") or "",
        url=payload["html_url"],
        state=payload["state"],
        labels=tuple(label["name"] for label in payload.get("labels", [])),
        milestone=payload["milestone"]["title"] if payload.get("milestone") else None,
    )


def list_issues_by_state(client: GitHubClient, repository: str, state: str) -> list[int]:
    """Return issue numbers for one repository state."""

    numbers: list[int] = []
    page = 1
    while True:
        payload = client.rest("GET", f"/repos/{repository}/issues?state={state}&per_page=100&page={page}")
        if not payload:
            return numbers
        numbers.extend(issue["number"] for issue in payload if "pull_request" not in issue)
        page += 1


def list_open_issues(client: GitHubClient, repository: str) -> list[int]:
    """Return all open issue numbers for a repository."""

    return list_issues_by_state(client, repository, "open")


def list_closed_issues(client: GitHubClient, repository: str) -> list[int]:
    """Return all closed issue numbers for a repository."""

    return list_issues_by_state(client, repository, "closed")


def list_all_issues(client: GitHubClient, repository: str) -> list[int]:
    """Return all issue numbers for a repository."""

    return list_issues_by_state(client, repository, "all")


def get_pull_request(client: GitHubClient, repository: str, number: int) -> PullRequest:
    """Fetch one pull request through the REST API."""

    payload = client.rest("GET", f"/repos/{repository}/pulls/{number}")
    return PullRequest(
        number=payload["number"],
        title=payload["title"],
        body=payload.get("body") or "",
        url=payload["html_url"],
        draft=payload.get("draft", False),
        merged=payload.get("merged", False),
    )
