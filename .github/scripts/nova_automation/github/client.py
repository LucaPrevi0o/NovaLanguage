"""Small GitHub REST and GraphQL client for Nova automation."""

from __future__ import annotations

import json
import os
import urllib.error
import urllib.request
from typing import Any

from .models import ProjectAutomationError


API_URL = os.environ.get("GITHUB_API_URL", "https://api.github.com")
GRAPHQL_URL = os.environ.get("GITHUB_GRAPHQL_URL", "https://api.github.com/graphql")


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
