"""GitHub API primitives used by Nova automation."""

from __future__ import annotations

from .client import API_URL, GRAPHQL_URL, GitHubClient, repository_parts, token_from_environment
from .models import (
    Issue,
    Project,
    ProjectAutomationError,
    ProjectField,
    ProjectIssueItem,
    ProjectItem,
    PullRequest,
)
from .repository import (
    get_issue,
    get_pull_request,
    list_all_issues,
    list_closed_issues,
    list_issues_by_state,
    list_open_issues,
)

__all__ = (
    "API_URL",
    "GRAPHQL_URL",
    "GitHubClient",
    "Issue",
    "Project",
    "ProjectAutomationError",
    "ProjectField",
    "ProjectIssueItem",
    "ProjectItem",
    "PullRequest",
    "get_issue",
    "get_pull_request",
    "list_all_issues",
    "list_closed_issues",
    "list_issues_by_state",
    "list_open_issues",
    "repository_parts",
    "token_from_environment",
)
