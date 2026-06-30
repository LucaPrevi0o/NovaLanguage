"""GitHub API data models for Nova automation."""

from __future__ import annotations

from dataclasses import dataclass


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
