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
from typing import Any


API_URL = os.environ.get("GITHUB_API_URL", "https://api.github.com")
GRAPHQL_URL = os.environ.get("GITHUB_GRAPHQL_URL", "https://api.github.com/graphql")
PROJECT_OWNER = os.environ.get("PROJECT_OWNER", "LucaPrevi0o")
PROJECT_NUMBER = int(os.environ.get("PROJECT_NUMBER", "4"))
PROJECT_TITLE = os.environ.get("PROJECT_TITLE", "Nova - Development Roadmap")

STATUS_FIELD = "Status"
PHASE_FIELD = "Phase"
KIND_FIELD = "Kind"
PRIORITY_FIELD = "Priority"
SIZE_FIELD = "Size"

METADATA_HEADER = "Project metadata"


PHASE_ALIASES = {
    "project workflow": "Project workflow",
    "phase 4 - semantic analysis split": "4 - Semantic analysis split",
    "4 - semantic analysis split": "4 - Semantic analysis split",
    "phase 5 - type model": "5 - Type model",
    "5 - type model": "5 - Type model",
    "phase 6 - multi-file project pipeline": "6 - Multi-file project pipeline",
    "6 - multi-file project pipeline": "6 - Multi-file project pipeline",
    "phase 7 - standard library as source": "7 - Standard library",
    "phase 7 - standard library": "7 - Standard library",
    "7 - standard library": "7 - Standard library",
    "phase 8 - ir preparation": "8 - IR preparation",
    "8 - ir preparation": "8 - IR preparation",
    "phase 9 - advanced nova features": "9 - Advanced features",
    "phase 9 - advanced features": "9 - Advanced features",
    "9 - advanced features": "9 - Advanced features",
    "future development": "Future development",
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

    phase = metadata.get("Phase")
    if phase:
        canonical[PHASE_FIELD] = PHASE_ALIASES.get(normalize_key(phase), phase.strip())

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

    canonical = canonical_metadata(metadata)
    project = get_project(client)
    item_id = add_issue_to_project(client, project, issue)

    updated = False
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


def build_parser() -> argparse.ArgumentParser:
    """Build the command-line parser."""

    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    sync_parser = subparsers.add_parser("sync-issue", help="Sync issue metadata into Project fields")
    sync_parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY"), help="Repository in owner/name form")
    sync_parser.add_argument("--issue-number", type=int, help="Issue number to sync")
    sync_parser.add_argument("--all-open", action="store_true", help="Sync all open issues")
    sync_parser.add_argument("--strict", action="store_true", help="Fail on missing or invalid issue metadata")
    sync_parser.set_defaults(func=sync_issue_command)

    pr_parser = subparsers.add_parser("sync-pr-status", help="Sync Project issue status from pull request state")
    pr_parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY"), help="Repository in owner/name form")
    pr_parser.add_argument("--pr-number", type=int, required=True, help="Pull request number to inspect")
    pr_parser.set_defaults(func=sync_pr_status_command)

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
