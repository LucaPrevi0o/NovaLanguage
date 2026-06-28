#!/usr/bin/env python3
"""Shared GitHub Project v2 board helpers for Nova automation scripts."""

from __future__ import annotations

import os
from typing import Any

from .project_github import (
    GitHubClient,
    Issue,
    Project,
    ProjectAutomationError,
    ProjectField,
    ProjectIssueItem,
    ProjectItem,
    get_issue,
)
from .project_metadata import DONE_STATUS, STATUS_FIELD


PROJECT_OWNER = os.environ.get("PROJECT_OWNER", "LucaPrevi0o")
PROJECT_NUMBER = int(os.environ.get("PROJECT_NUMBER", "4"))


def project_field_values(value_nodes: list[dict[str, Any]]) -> dict[str, str]:
    """Return Project item field values keyed by field name."""

    fields: dict[str, str] = {}
    for value in value_nodes:
        field = value.get("field")
        if not field:
            continue
        fields[field["name"]] = value.get("name") or value.get("text") or ""
    return fields


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
                      id
                      isArchived
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
            items.append(ProjectItem(
                id=node["id"],
                number=content.get("number"),
                title=content.get("title", ""),
                url=content.get("url"),
                state=content.get("state"),
                milestone=content["milestone"]["title"] if content.get("milestone") else None,
                archived=node["isArchived"],
                fields=project_field_values(node["fieldValues"]["nodes"]),
            ))
        if not project_items["pageInfo"]["hasNextPage"]:
            return items
        cursor = project_items["pageInfo"]["endCursor"]


def project_issue_item(client: GitHubClient, project: Project, issue: Issue) -> ProjectIssueItem | None:
    """Return the Project item for an issue, including archived items."""

    data = client.graphql(
        """
        query($issueId: ID!) {
          node(id: $issueId) {
            ... on Issue {
              projectItems(first: 50, includeArchived: true) {
                nodes {
                  id
                  isArchived
                  project {
                    id
                    title
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
        {"issueId": issue.node_id},
    )
    for node in data["node"]["projectItems"]["nodes"]:
        if node["project"]["id"] == project.id:
            return ProjectIssueItem(
                id=node["id"],
                archived=node["isArchived"],
                fields=project_field_values(node["fieldValues"]["nodes"]),
            )
    return None


def project_item_id(client: GitHubClient, project: Project, issue: Issue) -> str | None:
    """Return the Project item ID for an issue, if it is already in the Project."""

    item = project_issue_item(client, project, issue)
    return item.id if item else None


def project_item_id_for_content(client: GitHubClient, project: Project, content_id: str) -> str | None:
    """Return one Project item for an issue or pull request content ID."""

    data = client.graphql(
        """
        query($contentId: ID!) {
          node(id: $contentId) {
            ... on Issue { projectItems(first: 50, includeArchived: true) { nodes { id project { id } } } }
            ... on PullRequest { projectItems(first: 50, includeArchived: true) { nodes { id project { id } } } }
          }
        }
        """,
        {"contentId": content_id},
    )
    node = data.get("node")
    if not node or "projectItems" not in node:
        return None
    for item in node["projectItems"]["nodes"]:
        if item["project"]["id"] == project.id:
            return item["id"]
    return None


def add_content_to_project(client: GitHubClient, project: Project, content_id: str) -> str:
    """Add issue or pull request content to the Project and return the item ID."""

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
        {"projectId": project.id, "contentId": content_id},
    )
    return data["addProjectV2ItemById"]["item"]["id"]


def add_issue_to_project(client: GitHubClient, project: Project, issue: Issue) -> str:
    """Add an issue to the Project and return the Project item ID."""

    existing = project_item_id(client, project, issue)
    if existing:
        return existing
    return add_content_to_project(client, project, issue.node_id)


def archive_project_item(client: GitHubClient, project: Project, item_id: str) -> None:
    """Archive one Project item."""

    client.graphql(
        """
        mutation($projectId: ID!, $itemId: ID!) {
          archiveProjectV2Item(input: {projectId: $projectId, itemId: $itemId}) {
            item {
              id
            }
          }
        }
        """,
        {"projectId": project.id, "itemId": item_id},
    )


def unarchive_project_item(client: GitHubClient, project: Project, item_id: str) -> None:
    """Unarchive one Project item."""

    client.graphql(
        """
        mutation($projectId: ID!, $itemId: ID!) {
          unarchiveProjectV2Item(input: {projectId: $projectId, itemId: $itemId}) {
            item {
              id
            }
          }
        }
        """,
        {"projectId": project.id, "itemId": item_id},
    )


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


def set_date_value(client: GitHubClient, project: Project, item_id: str, field: ProjectField, value: str) -> None:
    """Set one Project date field value."""

    client.graphql(
        """
        mutation($projectId: ID!, $itemId: ID!, $fieldId: ID!, $date: Date!) {
          updateProjectV2ItemFieldValue(input: {
            projectId: $projectId,
            itemId: $itemId,
            fieldId: $fieldId,
            value: { date: $date }
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
            "date": value,
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


def sync_issue_archive(client: GitHubClient, repository: str, issue_number: int) -> bool:
    """Sync one issue's archived Project state from its open/closed state."""

    issue = get_issue(client, repository, issue_number)
    project = get_project(client)
    item = project_issue_item(client, project, issue)

    if item is None:
        if issue.state == "open":
            add_issue_to_project(client, project, issue)
            print(f"Added open #{issue.number} to Project; it is visible by default")
            return True
        print(f"::warning::Closed #{issue.number} is not in the Project; cannot archive it")
        return False

    if issue.state == "closed":
        status = item.fields.get(STATUS_FIELD)
        if status != DONE_STATUS:
            visible_status = status or "unset"
            print(
                f"::warning::Closed #{issue.number} has Project status '{visible_status}', "
                f"not '{DONE_STATUS}'; not archiving"
            )
            return False
        if item.archived:
            print(f"#{issue.number} is already archived")
            return False
        archive_project_item(client, project, item.id)
        print(f"Archived #{issue.number} from Project")
        return True

    if issue.state == "open":
        if not item.archived:
            print(f"#{issue.number} is already visible in Project")
            return False
        unarchive_project_item(client, project, item.id)
        print(f"Unarchived #{issue.number} in Project")
        return True

    print(f"::warning::Issue #{issue.number} has unsupported state '{issue.state}'; not changing archive state")
    return False
