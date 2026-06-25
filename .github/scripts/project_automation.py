#!/usr/bin/env python3
"""GitHub Project automation helpers for NovaLanguage."""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from urllib.parse import quote


API_URL = os.environ.get("GITHUB_API_URL", "https://api.github.com")
GRAPHQL_URL = os.environ.get("GITHUB_GRAPHQL_URL", "https://api.github.com/graphql")
PROJECT_OWNER = os.environ.get("PROJECT_OWNER", "LucaPrevi0o")
PROJECT_NUMBER = int(os.environ.get("PROJECT_NUMBER", "4"))
PROJECT_TITLE = os.environ.get("PROJECT_TITLE", "Nova - Development Roadmap")

STATUS_FIELD = "Status"
KIND_FIELD = "Kind"
PRIORITY_FIELD = "Priority"
SIZE_FIELD = "Size"
LEGACY_PHASE_FIELD = "Phase"

METADATA_HEADER = "Project metadata"


MILESTONE_ALIASES = {
    "project workflow": "Project workflow",
    "phase 4 - semantic analysis split": "Phase 4 - Semantic analysis split",
    "4 - semantic analysis split": "Phase 4 - Semantic analysis split",
    "phase 5 - type model": "Phase 5 - Type model",
    "5 - type model": "Phase 5 - Type model",
    "phase 6 - multi-file project pipeline": "Phase 6 - Multi-file project pipeline",
    "6 - multi-file project pipeline": "Phase 6 - Multi-file project pipeline",
    "phase 7 - standard library as source": "Phase 7 - Standard library",
    "phase 7 - standard library": "Phase 7 - Standard library",
    "7 - standard library": "Phase 7 - Standard library",
    "phase 8 - ir preparation": "Phase 8 - IR preparation",
    "8 - ir preparation": "Phase 8 - IR preparation",
    "phase 9 - advanced nova features": "Phase 9 - Advanced features",
    "phase 9 - advanced features": "Phase 9 - Advanced features",
    "9 - advanced features": "Phase 9 - Advanced features",
    "future development": "Future development",
}

MILESTONE_DESCRIPTIONS = {
    "Project workflow": "Repository automation, issue tracking, documentation publishing, and project-management workflow work.",
    "Phase 4 - Semantic analysis split": "Parser and semantic-analysis boundary work from Phase 4 of the Nova roadmap.",
    "Phase 5 - Type model": "TypeSyntax, TypeSymbol, and type-model groundwork from Phase 5 of the Nova roadmap.",
    "Phase 6 - Multi-file project pipeline": "Project-level compiler front-end pipeline work from Phase 6 of the Nova roadmap.",
    "Phase 7 - Standard library": "Semantic standard-library and source-backed Core loading work from Phase 7 of the Nova roadmap.",
    "Phase 8 - IR preparation": "Backend-neutral lowering and IR preparation work from Phase 8 of the Nova roadmap.",
    "Phase 9 - Advanced features": "Deferred advanced Nova language features from Phase 9 of the Nova roadmap.",
    "Future development": "Future-facing design and maintenance work that is not part of the current compiler phase.",
}

PRIORITY_ALIASES = {
    "p0": "0 - Blocks progress",
    "0": "0 - Blocks progress",
    "0 - blocks progress": "0 - Blocks progress",
    "p1": "1 - Important next step",
    "1": "1 - Important next step",
    "1 - important next step": "1 - Important next step",
    "p2": "2 - Possible next focus",
    "2": "2 - Possible next focus",
    "2 - possible next focus": "2 - Possible next focus",
    "p3": "3 - Later addition",
    "3": "3 - Later addition",
    "3 - later addition": "3 - Later addition",
}

STATUS_ALIASES = {
    "backlog": "Backlog",
    "ready": "Ready",
    "in progress": "In Progress",
    "blocked": "Blocked",
    "in review": "In Review",
    "done": "Done",
}

MANAGED_KIND_LABELS = {
    "bug": ("bug", "d73a4a", "Detected functional problem."),
    "feature": ("feature", "a2eeef", "New feature or request."),
    "refactor": ("refactor", "1d76db", "Code restructuring without changing intended behavior."),
    "design": ("design", "7b61ff", "Architecture or language/compiler design work."),
    "research": ("research", "fbca04", "Research or investigation before implementation."),
    "test": ("test", "f9d0c4", "Testing work or regression coverage."),
    "docs": ("docs", "0075ca", "Documentation work."),
}

PHASE_NUMBER_TO_MILESTONE = {
    4: "Phase 4 - Semantic analysis split",
    5: "Phase 5 - Type model",
    6: "Phase 6 - Multi-file project pipeline",
    7: "Phase 7 - Standard library",
    8: "Phase 8 - IR preparation",
    9: "Phase 9 - Advanced features",
}

ACTIVE_STATUSES = {"Ready", "In Progress", "In Review"}
DONE_STATUS = "Done"
HIGH_PRIORITY = "1 - Important next step"

STOPWORDS = {
    "a", "an", "and", "are", "as", "at", "be", "between", "by", "can", "from",
    "has", "in", "into", "is", "it", "keep", "later", "of", "or", "should",
    "that", "the", "to", "with", "work",
}


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

    number: int | None
    title: str
    url: str | None
    state: str | None
    milestone: str | None
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


def parse_metadata(body: str) -> dict[str, str]:
    """Parse the issue body's Project metadata block."""

    metadata: dict[str, str] = {}
    in_block = False
    header_pattern = re.compile(r"^##\s+(.+?)\s*$")
    item_pattern = re.compile(r"^-\s*([^:]+):\s*(.*?)\s*$")

    for line in body.splitlines():
        header_match = header_pattern.match(line)
        if header_match:
            heading = header_match.group(1).strip()
            if heading == METADATA_HEADER:
                in_block = True
                continue
            if in_block:
                break

        if not in_block:
            continue

        item_match = item_pattern.match(line)
        if item_match:
            metadata[item_match.group(1).strip()] = item_match.group(2).strip()

    return metadata


def normalize_key(value: str) -> str:
    """Normalize a metadata value for option matching."""

    return re.sub(r"\s+", " ", value.strip().lower())


def first_kind(value: str) -> str:
    """Use the first kind in values such as 'docs / design'."""

    return value.split("/", 1)[0].strip()


def canonical_metadata(metadata: dict[str, str]) -> dict[str, str]:
    """Convert issue metadata into Project field option names."""

    canonical: dict[str, str] = {}

    kind = metadata.get("Kind")
    if kind:
        canonical[KIND_FIELD] = normalize_key(first_kind(kind))

    priority = metadata.get("Priority")
    if priority:
        canonical[PRIORITY_FIELD] = PRIORITY_ALIASES.get(normalize_key(priority), priority.strip())

    size = metadata.get("Size")
    if size:
        canonical[SIZE_FIELD] = size.strip().upper()

    status = metadata.get("Suggested status")
    if status:
        canonical[STATUS_FIELD] = STATUS_ALIASES.get(normalize_key(status), status.strip())

    return canonical


def milestone_from_metadata(metadata: dict[str, str]) -> str | None:
    """Return the issue milestone represented by metadata."""

    milestone = metadata.get("Milestone") or metadata.get("Phase")
    if not milestone:
        return None
    return MILESTONE_ALIASES.get(normalize_key(milestone), milestone.strip())


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
        labels=tuple(label["name"] for label in payload.get("labels", [])),
        milestone=payload["milestone"]["title"] if payload.get("milestone") else None,
    )


def list_open_issues(client: GitHubClient, repository: str) -> list[int]:
    """Return all open issue numbers for a repository."""

    numbers: list[int] = []
    page = 1
    while True:
        payload = client.rest("GET", f"/repos/{repository}/issues?state=open&per_page=100&page={page}")
        if not payload:
            return numbers
        numbers.extend(issue["number"] for issue in payload if "pull_request" not in issue)
        page += 1


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


def list_milestones(client: GitHubClient, repository: str) -> dict[str, int]:
    """Return repository milestones keyed by title."""

    milestones: dict[str, int] = {}
    page = 1
    while True:
        payload = client.rest("GET", f"/repos/{repository}/milestones?state=all&per_page=100&page={page}")
        if not payload:
            return milestones
        for milestone in payload:
            milestones[milestone["title"]] = milestone["number"]
        page += 1


def ensure_milestone(client: GitHubClient, repository: str, title: str) -> int:
    """Return a repository milestone number, creating known roadmap milestones when missing."""

    milestones = list_milestones(client, repository)
    existing = milestones.get(title)
    if existing is not None:
        return existing

    description = MILESTONE_DESCRIPTIONS.get(title)
    if description is None:
        raise ProjectAutomationError(f"Milestone '{title}' was not found and is not a managed roadmap milestone")

    payload = client.rest("POST", f"/repos/{repository}/milestones", {
        "title": title,
        "description": description,
    })
    print(f"Created milestone '{title}'")
    return payload["number"]


def set_issue_milestone(client: GitHubClient, repository: str, issue: Issue, milestone_title: str) -> bool:
    """Set an issue milestone and return whether the issue changed."""

    if issue.milestone == milestone_title:
        print(f"#{issue.number} already has milestone '{milestone_title}'")
        return False

    milestone_number = ensure_milestone(client, repository, milestone_title)
    client.rest("PATCH", f"/repos/{repository}/issues/{issue.number}", {"milestone": milestone_number})
    print(f"Synced #{issue.number}: Milestone = {milestone_title}")
    return True


def remove_project_field(client: GitHubClient, field_name: str) -> bool:
    """Remove one Project field and return whether it existed."""

    project = get_project(client)
    field = project.fields.get(field_name)
    if field is None:
        print(f"Project field '{field_name}' was not found; no cleanup needed")
        return False

    client.graphql(
        """
        mutation($fieldId: ID!) {
          deleteProjectV2Field(input: {fieldId: $fieldId}) {
            clientMutationId
          }
        }
        """,
        {"fieldId": field.id},
    )
    print(f"Removed Project field '{field_name}'")
    return True


def get_project(client: GitHubClient) -> Project:
    """Fetch the configured user-owned Project and its fields."""

    data = client.graphql(
        """
        query($owner: String!, $number: Int!) {
          user(login: $owner) {
            projectV2(number: $number) {
              id
              title
              fields(first: 50) {
                nodes {
                  ... on ProjectV2FieldCommon {
                    id
                    name
                  }
                  ... on ProjectV2SingleSelectField {
                    options {
                      id
                      name
                    }
                  }
                }
              }
            }
          }
        }
        """,
        {"owner": PROJECT_OWNER, "number": PROJECT_NUMBER},
    )
    project_data = data["user"]["projectV2"]
    if project_data is None:
        raise ProjectAutomationError(f"Project {PROJECT_OWNER}/{PROJECT_NUMBER} was not found")

    fields: dict[str, ProjectField] = {}
    for node in project_data["fields"]["nodes"]:
        if node is None:
            continue
        options = {option["name"]: option["id"] for option in node.get("options", [])}
        fields[node["name"]] = ProjectField(node["id"], node["name"], options)

    return Project(project_data["id"], project_data["title"], fields)


def list_project_items(client: GitHubClient) -> list[ProjectItem]:
    """Return all issue items from the configured Project."""

    items: list[ProjectItem] = []
    cursor: str | None = None
    while True:
        data = client.graphql(
            """
            query($owner: String!, $number: Int!, $cursor: String) {
              user(login: $owner) {
                projectV2(number: $number) {
                  items(first: 100, after: $cursor) {
                    pageInfo {
                      hasNextPage
                      endCursor
                    }
                    nodes {
                      content {
                        ... on Issue {
                          number
                          title
                          state
                          url
                          milestone {
                            title
                          }
                        }
                      }
                      fieldValues(first: 50) {
                        nodes {
                          ... on ProjectV2ItemFieldSingleSelectValue {
                            name
                            field {
                              ... on ProjectV2FieldCommon {
                                name
                              }
                            }
                          }
                          ... on ProjectV2ItemFieldTextValue {
                            text
                            field {
                              ... on ProjectV2FieldCommon {
                                name
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """,
            {"owner": PROJECT_OWNER, "number": PROJECT_NUMBER, "cursor": cursor},
        )
        project_items = data["user"]["projectV2"]["items"]
        for node in project_items["nodes"]:
            content = node.get("content")
            if not content:
                continue
            fields: dict[str, str] = {}
            for value in node["fieldValues"]["nodes"]:
                field = value.get("field")
                if not field:
                    continue
                fields[field["name"]] = value.get("name") or value.get("text") or ""
            items.append(ProjectItem(
                number=content.get("number"),
                title=content.get("title", ""),
                url=content.get("url"),
                state=content.get("state"),
                milestone=content["milestone"]["title"] if content.get("milestone") else None,
                fields=fields,
            ))
        if not project_items["pageInfo"]["hasNextPage"]:
            return items
        cursor = project_items["pageInfo"]["endCursor"]


def project_item_id(client: GitHubClient, project: Project, issue: Issue) -> str | None:
    """Return the Project item ID for an issue, if it is already in the Project."""

    data = client.graphql(
        """
        query($issueId: ID!) {
          node(id: $issueId) {
            ... on Issue {
              projectItems(first: 50) {
                nodes {
                  id
                  project {
                    id
                    title
                  }
                }
              }
            }
          }
        }
        """,
        {"issueId": issue.node_id},
    )
    for node in data["node"]["projectItems"]["nodes"]:
        if node["project"]["id"] == project.id:
            return node["id"]
    return None


def add_issue_to_project(client: GitHubClient, project: Project, issue: Issue) -> str:
    """Add an issue to the Project and return the Project item ID."""

    existing = project_item_id(client, project, issue)
    if existing:
        return existing

    data = client.graphql(
        """
        mutation($projectId: ID!, $contentId: ID!) {
          addProjectV2ItemById(input: { projectId: $projectId, contentId: $contentId }) {
            item {
              id
            }
          }
        }
        """,
        {"projectId": project.id, "contentId": issue.node_id},
    )
    return data["addProjectV2ItemById"]["item"]["id"]


def set_single_select_value(client: GitHubClient, project: Project, item_id: str, field: ProjectField, option_id: str) -> None:
    """Set one Project single-select value."""

    client.graphql(
        """
        mutation($projectId: ID!, $itemId: ID!, $fieldId: ID!, $optionId: String!) {
          updateProjectV2ItemFieldValue(input: {
            projectId: $projectId,
            itemId: $itemId,
            fieldId: $fieldId,
            value: { singleSelectOptionId: $optionId }
          }) {
            projectV2Item {
              id
            }
          }
        }
        """,
        {
            "projectId": project.id,
            "itemId": item_id,
            "fieldId": field.id,
            "optionId": option_id,
        },
    )


def set_issue_status(client: GitHubClient, repository: str, issue_number: int, status_name: str) -> None:
    """Set the roadmap Project status for one issue."""

    issue = get_issue(client, repository, issue_number)
    project = get_project(client)
    field = project.fields.get(STATUS_FIELD)
    if field is None:
        raise ProjectAutomationError(f"Project field '{STATUS_FIELD}' was not found")
    option_id = field.options.get(status_name)
    if option_id is None:
        raise ProjectAutomationError(f"Project status option '{status_name}' was not found")
    item_id = add_issue_to_project(client, project, issue)
    set_single_select_value(client, project, item_id, field, option_id)
    print(f"Set #{issue_number} status to {status_name}")


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


def sync_issue(client: GitHubClient, repository: str, issue_number: int, strict: bool) -> bool:
    """Sync one issue's metadata into Project fields."""

    issue = get_issue(client, repository, issue_number)
    metadata = parse_metadata(issue.body)
    if not metadata:
        message = f"#{issue.number} has no '## {METADATA_HEADER}' block; skipping"
        if strict:
            raise ProjectAutomationError(message)
        print(f"::warning::{message}")
        return False

    milestone_title = milestone_from_metadata(metadata)
    if not milestone_title:
        message = f"#{issue.number} has no Milestone metadata; legacy Phase metadata is also absent"
        if strict:
            raise ProjectAutomationError(message)
        print(f"::warning::{message}")

    canonical = canonical_metadata(metadata)
    project = get_project(client)
    item_id = add_issue_to_project(client, project, issue)

    updated = False
    if milestone_title:
        updated = set_issue_milestone(client, repository, issue, milestone_title)

    for field_name, option_name in canonical.items():
        field = project.fields.get(field_name)
        if field is None:
            raise ProjectAutomationError(f"Project field '{field_name}' was not found")
        option_id = field.options.get(option_name)
        if option_id is None:
            message = f"Project field '{field_name}' has no option '{option_name}' for #{issue.number}"
            if strict:
                raise ProjectAutomationError(message)
            print(f"::warning::{message}")
            continue
        set_single_select_value(client, project, item_id, field, option_id)
        updated = True
        print(f"Synced #{issue.number}: {field_name} = {option_name}")

    if not updated:
        print(f"::warning::No Project fields were updated for #{issue.number}")
    return updated


def kind_label_from_metadata(metadata: dict[str, str]) -> str | None:
    """Return the managed kind label represented by issue metadata."""

    kind = metadata.get("Kind")
    if not kind:
        return None
    kind_key = normalize_key(first_kind(kind))
    label = MANAGED_KIND_LABELS.get(kind_key)
    return label[0] if label else kind_key


def sync_labels_for_issue(client: GitHubClient, repository: str, issue_number: int, strict: bool) -> bool:
    """Sync one issue's managed kind label from its metadata block."""

    issue = get_issue(client, repository, issue_number)
    metadata = parse_metadata(issue.body)
    if not metadata:
        message = f"#{issue.number} has no '## {METADATA_HEADER}' block; skipping label sync"
        if strict:
            raise ProjectAutomationError(message)
        print(f"::warning::{message}")
        return False

    target_label = kind_label_from_metadata(metadata)
    if not target_label:
        message = f"#{issue.number} has no Kind metadata; skipping label sync"
        if strict:
            raise ProjectAutomationError(message)
        print(f"::warning::{message}")
        return False

    managed_labels = {entry[0] for entry in MANAGED_KIND_LABELS.values()}
    if target_label not in managed_labels:
        message = f"Kind '{target_label}' is not a managed label"
        if strict:
            raise ProjectAutomationError(message)
        print(f"::warning::{message}")
        return False

    label_name, color, description = MANAGED_KIND_LABELS[target_label]
    ensure_label(client, repository, label_name, color, description)

    changed = False
    current_labels = set(issue.labels)
    for stale_label in sorted((current_labels & managed_labels) - {label_name}):
        remove_issue_label(client, repository, issue.number, stale_label)
        print(f"Removed stale managed label '{stale_label}' from #{issue.number}")
        changed = True

    if label_name not in current_labels:
        add_issue_label(client, repository, issue.number, label_name)
        print(f"Added label '{label_name}' to #{issue.number}")
        changed = True

    if not changed:
        print(f"#{issue.number} already has managed label '{label_name}'")
    return changed


def issue_reference_numbers(repository: str, text: str) -> set[int]:
    """Extract same-repository issue references from text."""

    owner, name = repository_parts(repository)
    references: set[int] = set()

    plain_pattern = re.compile(r"(?<![\w/])#(\d+)\b")
    repo_pattern = re.compile(rf"(?<![\w/]){re.escape(owner)}/{re.escape(name)}#(\d+)\b", re.IGNORECASE)
    url_pattern = re.compile(
        rf"https://github\.com/{re.escape(owner)}/{re.escape(name)}/issues/(\d+)\b",
        re.IGNORECASE,
    )

    for pattern in (plain_pattern, repo_pattern, url_pattern):
        references.update(int(match.group(1)) for match in pattern.finditer(text or ""))

    return references


def closing_issue_reference_numbers(repository: str, text: str) -> set[int]:
    """Extract issue references from lines that use GitHub closing keywords."""

    closing_keywords = re.compile(r"\b(close[sd]?|fix(e[sd])?|resolve[sd]?)\b", re.IGNORECASE)
    references: set[int] = set()
    for line in (text or "").splitlines():
        for match in closing_keywords.finditer(line):
            references.update(issue_reference_numbers(repository, line[match.end():]))
    return references


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
        set_issue_status(client, args.repo, issue_number, target_status)
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


def phase_number_from_text(text: str) -> int | None:
    """Extract a roadmap phase number from text."""

    match = re.search(r"\bPhase\s+(\d+)\b", text, re.IGNORECASE)
    return int(match.group(1)) if match else None


def current_focus_phase(plan_text: str) -> int | None:
    """Return the phase number from PLAN.md current focus."""

    match = re.search(r"^Current focus:\s*(.+)$", plan_text, re.MULTILINE)
    return phase_number_from_text(match.group(1)) if match else None


def readme_focus_phase(readme_text: str) -> int | None:
    """Return the phase number from README current focus."""

    match = re.search(r"^Current focus:\s*\*\*(.+?)\*\*\.", readme_text, re.MULTILINE)
    return phase_number_from_text(match.group(1)) if match else None


def phase_statuses(plan_text: str) -> dict[int, str]:
    """Parse PLAN.md phase status table."""

    statuses: dict[int, str] = {}
    pattern = re.compile(r"^\|\s*(\d+)\.\s+[^|]+\|\s*([^|]+?)\s*\|", re.MULTILINE)
    for match in pattern.finditer(plan_text):
        statuses[int(match.group(1))] = match.group(2).strip()
    return statuses


def immediate_next_steps(plan_text: str) -> list[str]:
    """Parse PLAN.md immediate next steps."""

    match = re.search(r"^## Immediate Next Steps\s*(.+)$", plan_text, re.MULTILINE | re.DOTALL)
    if not match:
        return []
    steps: list[str] = []
    for line in match.group(1).splitlines():
        step_match = re.match(r"^\d+\.\s+(.+?)\s*$", line)
        if step_match:
            steps.append(step_match.group(1))
    return steps


def terms(text: str) -> set[str]:
    """Extract normalized search terms for loose issue matching."""

    return {
        token
        for token in re.findall(r"[a-zA-Z][a-zA-Z0-9]+", text.lower())
        if len(token) > 2 and token not in STOPWORDS
    }


def best_issue_match(step: str, items: list[ProjectItem]) -> ProjectItem | None:
    """Find a likely issue for an immediate next step."""

    step_terms = terms(step)
    best_item: ProjectItem | None = None
    best_score = 0
    for item in items:
        if item.state != "OPEN":
            continue
        score = len(step_terms & terms(item.title))
        if score > best_score:
            best_score = score
            best_item = item
    return best_item if best_score >= 2 else None


def check_plan_drift_command(args: argparse.Namespace) -> int:
    """Run the check-plan-drift subcommand."""

    client = GitHubClient(token_from_environment())
    plan_text = Path(args.plan).read_text(encoding="utf-8")
    readme_text = Path(args.readme).read_text(encoding="utf-8") if args.readme else ""
    items = list_project_items(client)

    errors: list[str] = []
    warnings: list[str] = []
    notices: list[str] = []

    plan_focus = current_focus_phase(plan_text)
    readme_focus = readme_focus_phase(readme_text) if readme_text else None
    if plan_focus is None:
        errors.append("PLAN.md does not declare a current focus phase")
    if readme_text and readme_focus is None:
        warnings.append("README.md does not declare a current focus phase")
    if plan_focus is not None and readme_focus is not None and plan_focus != readme_focus:
        errors.append(f"README current focus Phase {readme_focus} does not match PLAN.md Phase {plan_focus}")

    if plan_focus is not None:
        focus_milestone = PHASE_NUMBER_TO_MILESTONE.get(plan_focus)
        active_focus_items = [
            item for item in items
            if item.milestone == focus_milestone and item.fields.get(STATUS_FIELD) in ACTIVE_STATUSES
        ]
        in_progress_focus_items = [
            item for item in active_focus_items
            if item.fields.get(STATUS_FIELD) == "In Progress"
        ]
        if not active_focus_items:
            errors.append(f"No active Project item found for PLAN.md current focus Phase {plan_focus}")
        elif not in_progress_focus_items:
            warnings.append(f"PLAN.md current focus Phase {plan_focus} has active items but none marked In Progress")
        else:
            labels = ", ".join(f"#{item.number} {item.title}" for item in in_progress_focus_items)
            notices.append(f"Current focus is represented by {labels}")

    statuses = phase_statuses(plan_text)
    for phase, status in statuses.items():
        if status != "Complete":
            continue
        phase_milestone = PHASE_NUMBER_TO_MILESTONE.get(phase)
        if not phase_milestone:
            continue
        stale_items = [
            item for item in items
            if item.milestone == phase_milestone
            and item.fields.get(PRIORITY_FIELD) == HIGH_PRIORITY
            and item.fields.get(STATUS_FIELD) != DONE_STATUS
        ]
        for item in stale_items:
            warnings.append(f"Phase {phase} is complete in PLAN.md but #{item.number} is still non-done P1 work")

    for step in immediate_next_steps(plan_text):
        match = best_issue_match(step, items)
        if match:
            notices.append(f"Immediate step maps to #{match.number}: {match.title}")
        else:
            notices.append(f"No direct issue match found for immediate step: {step}")

    for message in notices:
        print(f"::notice::{message}")
    for message in warnings:
        print(f"::warning::{message}")
    for message in errors:
        print(f"::error::{message}")

    if errors:
        return 1
    if warnings and args.fail_on_warning:
        return 1
    return 0


def remove_legacy_phase_field_command(args: argparse.Namespace) -> int:
    """Run the remove-legacy-phase-field subcommand."""

    if not args.confirm:
        raise ProjectAutomationError(
            "Refusing to remove the legacy Project Phase field without --confirm. "
            "Run this only after milestone-aware automation is merged to the default branch."
        )

    client = GitHubClient(token_from_environment())
    remove_project_field(client, LEGACY_PHASE_FIELD)
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
        help="Confirm that milestone-aware automation is already active on the default branch",
    )
    cleanup_parser.set_defaults(func=remove_legacy_phase_field_command)

    return parser


def main() -> int:
    """Run the selected automation command."""

    parser = build_parser()
    args = parser.parse_args()
    if not args.repo:
        parser.error("--repo is required when GITHUB_REPOSITORY is not set")
    try:
        return args.func(args)
    except ProjectAutomationError as error:
        print(f"::error::{error}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
