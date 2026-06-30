"""PLAN.md and Project roadmap drift checks."""

from __future__ import annotations

import argparse
import re
from pathlib import Path

from ..github import GitHubClient, ProjectItem, token_from_environment
from ..project.board import list_project_items
from ..project.metadata import (
    ACTIVE_STATUSES,
    DONE_STATUS,
    HIGH_PRIORITY,
    PHASE_NUMBER_TO_MILESTONE,
    PRIORITY_FIELD,
    SHARED_PHASE_MILESTONES,
    STATUS_FIELD,
)


STOPWORDS = {
    "a", "an", "and", "are", "as", "at", "be", "between", "by", "can", "from",
    "has", "in", "into", "is", "it", "keep", "later", "of", "or", "should",
    "that", "the", "to", "with", "work",
}


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
        if focus_milestone is None:
            notices.append(f"PLAN.md current focus Phase {plan_focus} is not tied to one roadmap milestone")
        else:
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
        if phase_milestone in SHARED_PHASE_MILESTONES:
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
