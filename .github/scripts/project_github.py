#!/usr/bin/env python3
"""Shared GitHub API helpers for Nova automation scripts."""

from __future__ import annotations

import json
import os
import urllib.error
import urllib.request
from dataclasses import dataclass
from typing import Any


API_URL = os.environ.get("GITHUB_API_URL", "https://api.github.com")
GRAPHQL_URL = os.environ.get("GITHUB_GRAPHQL_URL", "https://api.github.com/graphql")


class ProjectAutomationError(RuntimeError):
    """Raised when project automation cannot complete safely."""


@dataclass(frozen=True)
class Issue:
    """GitHub issue data needed by the project automation."""

    number: int
    node_id: str
    title: str
    body: str
    url: str
    state: str
    labels: tuple[str, ...] = ()
    milestone: str | None = None


@dataclass(frozen=True)
class ProjectField:
    """GitHub Project field metadata."""

    id: str
    name: str
    options: dict[str, str]


@dataclass(frozen=True)
class Project:
    """GitHub Project metadata used by sync operations."""

    id: str
    title: str
    fields: dict[str, ProjectField]


@dataclass(frozen=True)
class PullRequest:
    """GitHub pull request data needed by status automation."""

    number: int
    title: str
    body: str
    url: str
    draft: bool
    merged: bool


@dataclass(frozen=True)
class ProjectItem:
    """GitHub Project item data used by drift checks."""

    id: str
    number: int | None
    title: str
    url: str | None
    state: str | None
    milestone: str | None
    archived: bool
    fields: dict[str, str]


@dataclass(frozen=True)
class ProjectIssueItem:
    """One issue's item in the configured GitHub Project."""

    id: str
    archived: bool
    fields: dict[str, str]


class GitHubClient:
    """Small GitHub REST/GraphQL client backed by the workflow token."""

    def __init__(self, token: str) -> None:
        if not token:
            raise ProjectAutomationError(
                "Missing GitHub token. Set PROJECT_TOKEN with 'project' scope, or provide GITHUB_TOKEN "
                "for read-only/local checks."
            )
        self.token = token

    def rest(self, method: str, path: str, payload: dict[str, Any] | None = None) -> Any:
        """Call the GitHub REST API."""

        url = f"{API_URL.rstrip('/')}/{path.lstrip('/')}"
        data = None
        if payload is not None:
            data = json.dumps(payload).encode("utf-8")

        request = urllib.request.Request(
            url,
            data=data,
            method=method,
            headers={
                "Accept": "application/vnd.github+json",
                "Authorization": f"Bearer {self.token}",
                "X-GitHub-Api-Version": "2022-11-28",
                "User-Agent": "NovaLanguage-project-automation",
            },
        )
        return self._open_json(request)

    def graphql(self, query: str, variables: dict[str, Any] | None = None) -> Any:
        """Call the GitHub GraphQL API."""

        request = urllib.request.Request(
            GRAPHQL_URL,
            data=json.dumps({"query": query, "variables": variables or {}}).encode("utf-8"),
            method="POST",
            headers={
                "Accept": "application/vnd.github+json",
                "Authorization": f"Bearer {self.token}",
                "User-Agent": "NovaLanguage-project-automation",
            },
        )
        response = self._open_json(request)
        errors = response.get("errors")
        if errors:
            messages = "; ".join(error.get("message", str(error)) for error in errors)
            raise ProjectAutomationError(messages)
        return response["data"]

    def _open_json(self, request: urllib.request.Request) -> Any:
        try:
            with urllib.request.urlopen(request) as response:
                content = response.read().decode("utf-8")
        except urllib.error.HTTPError as error:
            detail = error.read().decode("utf-8")
            raise ProjectAutomationError(f"GitHub API {error.code}: {detail}") from error
        if not content:
            return None
        return json.loads(content)


def token_from_environment() -> str:
    """Return the best token available to the workflow."""

    return os.environ.get("PROJECT_TOKEN") or os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_TOKEN", "")


def repository_parts(repository: str) -> tuple[str, str]:
    """Split owner/name repository input."""

    if "/" not in repository:
        raise ProjectAutomationError(f"Repository must be owner/name, got '{repository}'")
    owner, name = repository.split("/", 1)
    return owner, name


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
