#!/usr/bin/env python3
"""Unit tests for Nova's shared GitHub Project board helpers."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path
from typing import Any
from unittest.mock import patch

SCRIPTS_DIR = Path(__file__).resolve().parents[1] / "scripts"
sys.path.insert(0, str(SCRIPTS_DIR))

from nova_automation.project_board import (  # noqa: E402
    add_issue_to_project,
    get_project,
    list_project_items,
    project_field_values,
    set_issue_status,
    sync_issue_archive,
)
from nova_automation.project_github import Issue, Project  # noqa: E402


class FakeGitHubClient:
    """Small fake for REST and GraphQL helper tests."""

    def __init__(self, *, rest: list[Any] | None = None, graphql: list[Any] | None = None) -> None:
        self.rest_responses = list(rest or [])
        self.graphql_responses = list(graphql or [])
        self.rest_calls: list[tuple[str, str, dict[str, Any] | None]] = []
        self.graphql_calls: list[tuple[str, dict[str, Any]]] = []

    def rest(self, method: str, path: str, payload: dict[str, Any] | None = None) -> Any:
        self.rest_calls.append((method, path, payload))
        if not self.rest_responses:
            raise AssertionError(f"Unexpected REST call: {method} {path}")
        return self.rest_responses.pop(0)

    def graphql(self, query: str, variables: dict[str, Any] | None = None) -> Any:
        self.graphql_calls.append((query, variables or {}))
        if not self.graphql_responses:
            raise AssertionError(f"Unexpected GraphQL call: {query}")
        return self.graphql_responses.pop(0)


def project_payload(field_nodes: list[dict[str, Any] | None] | None = None) -> dict[str, Any]:
    """Build a minimal Project v2 GraphQL response."""

    return {
        "user": {
            "projectV2": {
                "id": "P1",
                "title": "Nova - Development Roadmap",
                "fields": {"nodes": field_nodes or []},
            }
        }
    }


def issue_item_payload(
    *,
    project_id: str = "P1",
    item_id: str = "PVTI_1",
    archived: bool = False,
    fields: dict[str, str] | None = None,
) -> dict[str, Any]:
    """Build a minimal issue Project item GraphQL response."""

    field_nodes = [
        {"name": value, "field": {"name": name}}
        for name, value in (fields or {}).items()
    ]
    return {
        "node": {
            "projectItems": {
                "nodes": [
                    {
                        "id": item_id,
                        "isArchived": archived,
                        "project": {"id": project_id, "title": "Nova - Development Roadmap"},
                        "fieldValues": {"nodes": field_nodes},
                    }
                ]
            }
        }
    }


def issue_payload(*, state: str = "open") -> dict[str, Any]:
    """Build a minimal REST issue response."""

    return {
        "number": 63,
        "node_id": "I_63",
        "title": "Simplify automation",
        "body": "",
        "html_url": "https://github.com/LucaPrevi0o/NovaLanguage/issues/63",
        "state": state,
        "labels": [],
        "milestone": None,
    }


class ProjectBoardHelperTest(unittest.TestCase):
    """Coverage for reusable GitHub Project v2 board operations."""

    def test_project_field_values_maps_supported_field_value_nodes(self) -> None:
        self.assertEqual(
            project_field_values([
                {"name": "In Review", "field": {"name": "Status"}},
                {"text": "M", "field": {"name": "Size"}},
                {"name": "ignored"},
            ]),
            {"Status": "In Review", "Size": "M"},
        )

    def test_get_project_maps_fields_and_single_select_options(self) -> None:
        client = FakeGitHubClient(graphql=[
            project_payload([
                None,
                {
                    "id": "F_status",
                    "name": "Status",
                    "options": [
                        {"id": "O_review", "name": "In Review"},
                        {"id": "O_done", "name": "Done"},
                    ],
                },
                {"id": "F_start", "name": "Start date"},
            ])
        ])

        project = get_project(client)  # type: ignore[arg-type]

        self.assertEqual(project.id, "P1")
        self.assertEqual(project.fields["Status"].options["Done"], "O_done")
        self.assertEqual(project.fields["Start date"].options, {})

    def test_list_project_items_maps_issue_content_and_fields(self) -> None:
        client = FakeGitHubClient(graphql=[
            {
                "user": {
                    "projectV2": {
                        "items": {
                            "pageInfo": {"hasNextPage": False, "endCursor": None},
                            "nodes": [
                                {
                                    "id": "PVTI_1",
                                    "isArchived": False,
                                    "content": {
                                        "number": 63,
                                        "title": "Simplify automation",
                                        "state": "OPEN",
                                        "url": "https://github.com/LucaPrevi0o/NovaLanguage/issues/63",
                                        "milestone": {"title": "Future development"},
                                    },
                                    "fieldValues": {
                                        "nodes": [
                                            {"name": "In Review", "field": {"name": "Status"}},
                                        ]
                                    },
                                },
                                {"id": "PVTI_note", "isArchived": False, "content": None},
                            ],
                        }
                    }
                }
            }
        ])

        items = list_project_items(client)  # type: ignore[arg-type]

        self.assertEqual(len(items), 1)
        self.assertEqual(items[0].number, 63)
        self.assertEqual(items[0].milestone, "Future development")
        self.assertEqual(items[0].fields, {"Status": "In Review"})

    def test_add_issue_to_project_reuses_existing_project_item(self) -> None:
        client = FakeGitHubClient(graphql=[issue_item_payload()])
        project = Project(id="P1", title="Nova - Development Roadmap", fields={})
        issue = Issue(
            number=63,
            node_id="I_63",
            title="Simplify automation",
            body="",
            url="https://github.com/LucaPrevi0o/NovaLanguage/issues/63",
            state="open",
        )

        item_id = add_issue_to_project(client, project, issue)  # type: ignore[arg-type]

        self.assertEqual(item_id, "PVTI_1")
        self.assertEqual(len(client.graphql_calls), 1)

    def test_set_issue_status_updates_existing_project_item(self) -> None:
        client = FakeGitHubClient(
            rest=[issue_payload()],
            graphql=[
                project_payload([
                    {
                        "id": "F_status",
                        "name": "Status",
                        "options": [{"id": "O_review", "name": "In Review"}],
                    }
                ]),
                issue_item_payload(),
                {"updateProjectV2ItemFieldValue": {"projectV2Item": {"id": "PVTI_1"}}},
            ],
        )

        with patch("builtins.print") as print_mock:
            set_issue_status(client, "LucaPrevi0o/NovaLanguage", 63, "In Review")  # type: ignore[arg-type]

        self.assertEqual(client.graphql_calls[-1][1], {
            "projectId": "P1",
            "itemId": "PVTI_1",
            "fieldId": "F_status",
            "optionId": "O_review",
        })
        print_mock.assert_called_once_with("Set #63 status to In Review")

    def test_sync_issue_archive_archives_closed_done_item(self) -> None:
        client = FakeGitHubClient(
            rest=[issue_payload(state="closed")],
            graphql=[
                project_payload(),
                issue_item_payload(fields={"Status": "Done"}),
                {"archiveProjectV2Item": {"item": {"id": "PVTI_1"}}},
            ],
        )

        with patch("builtins.print") as print_mock:
            changed = sync_issue_archive(client, "LucaPrevi0o/NovaLanguage", 63)  # type: ignore[arg-type]

        self.assertTrue(changed)
        self.assertEqual(client.graphql_calls[-1][1], {"projectId": "P1", "itemId": "PVTI_1"})
        print_mock.assert_called_once_with("Archived #63 from Project")


if __name__ == "__main__":
    unittest.main()
