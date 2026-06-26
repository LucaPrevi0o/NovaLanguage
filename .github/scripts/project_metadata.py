#!/usr/bin/env python3
"""Shared project metadata constants for Nova GitHub automation."""

from __future__ import annotations

from typing import Iterable

STATUS_FIELD = "Status"
PRIORITY_FIELD = "Priority"
SIZE_FIELD = "Size"
LEGACY_PHASE_FIELD = "Phase"
LEGACY_KIND_FIELD = "Kind"

METADATA_HEADER = "Project metadata"
NO_RESPONSE = "_No response_"

MVP_MILESTONE = "Nova MVP compiler"
PROJECT_WORKFLOW_MILESTONE = "Project workflow"
FUTURE_MILESTONE = "Future development"

ADVANCED_FEATURE_MILESTONES = (
    "Advanced overload and override rules",
    "Access control",
    "Inheritance conflict checks",
    "Generics",
    "Bounded generics",
    "Class parameters",
    "Operator-overloadable Nova types",
    "Lambdas",
    "Variadic generics",
    "Monomorphization",
)

MANAGED_MILESTONES = (
    PROJECT_WORKFLOW_MILESTONE,
    MVP_MILESTONE,
    *ADVANCED_FEATURE_MILESTONES,
    FUTURE_MILESTONE,
)

MILESTONE_ALIASES = {
    "project workflow": PROJECT_WORKFLOW_MILESTONE,
    "nova mvp compiler": MVP_MILESTONE,
    "mvp compiler": MVP_MILESTONE,
    "first usable compiler": MVP_MILESTONE,
    "phase 1 - build health": MVP_MILESTONE,
    "1 - build health": MVP_MILESTONE,
    "phase 2 - parser semantics": MVP_MILESTONE,
    "2 - parser semantics": MVP_MILESTONE,
    "phase 3 - diagnostics": MVP_MILESTONE,
    "3 - diagnostics": MVP_MILESTONE,
    "phase 4 - semantic analysis split": MVP_MILESTONE,
    "4 - semantic analysis split": MVP_MILESTONE,
    "phase 5 - type model": MVP_MILESTONE,
    "5 - type model": MVP_MILESTONE,
    "phase 6 - multi-file project pipeline": MVP_MILESTONE,
    "6 - multi-file project pipeline": MVP_MILESTONE,
    "phase 7 - standard library as source": MVP_MILESTONE,
    "phase 7 - standard library": MVP_MILESTONE,
    "7 - standard library": MVP_MILESTONE,
    "phase 8 - ir preparation": MVP_MILESTONE,
    "8 - ir preparation": MVP_MILESTONE,
    "phase 9 - advanced nova features": FUTURE_MILESTONE,
    "phase 9 - advanced features": FUTURE_MILESTONE,
    "9 - advanced features": FUTURE_MILESTONE,
    "advanced overload and override rules": "Advanced overload and override rules",
    "access control": "Access control",
    "inheritance conflict checks": "Inheritance conflict checks",
    "generics": "Generics",
    "bounded generics": "Bounded generics",
    "class parameters": "Class parameters",
    "operator-overloadable nova types": "Operator-overloadable Nova types",
    "operator overloadable nova types": "Operator-overloadable Nova types",
    "lambdas": "Lambdas",
    "variadic generics": "Variadic generics",
    "monomorphization": "Monomorphization",
    "future development": FUTURE_MILESTONE,
}

MILESTONE_DESCRIPTIONS = {
    PROJECT_WORKFLOW_MILESTONE: (
        "Repository automation, issue tracking, documentation publishing, and project-management workflow work."
    ),
    MVP_MILESTONE: (
        "Phase 1 through Phase 8 work needed for the first usable Nova compiler front-end."
    ),
    "Advanced overload and override rules": "Post-MVP overload, override, specificity, and dispatch design work.",
    "Access control": "Post-MVP visibility and member-access enforcement work.",
    "Inheritance conflict checks": "Post-MVP inherited-member conflict and hierarchy validation work.",
    "Generics": "Post-MVP generic type and function support.",
    "Bounded generics": "Post-MVP generic constraint and bound support.",
    "Class parameters": "Post-MVP class parameter syntax, semantics, and lowering support.",
    "Operator-overloadable Nova types": "Post-MVP operator overload support for Nova-defined types.",
    "Lambdas": "Post-MVP lambda syntax, typing, capture, and lowering support.",
    "Variadic generics": "Post-MVP variadic type parameter support.",
    "Monomorphization": "Post-MVP generic specialization and monomorphization support.",
    FUTURE_MILESTONE: "Future-facing design and maintenance work that is not part of the current compiler phase.",
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

SIZE_OPTIONS = ("XS", "S", "M", "L", "XL")

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
    1: MVP_MILESTONE,
    2: MVP_MILESTONE,
    3: MVP_MILESTONE,
    4: MVP_MILESTONE,
    5: MVP_MILESTONE,
    6: MVP_MILESTONE,
    7: MVP_MILESTONE,
    8: MVP_MILESTONE,
}
SHARED_PHASE_MILESTONES = {MVP_MILESTONE}

ACTIVE_STATUSES = {"Ready", "In Progress", "In Review"}
DONE_STATUS = "Done"
HIGH_PRIORITY = "1 - Important next step"


def unique_values(values: Iterable[str]) -> tuple[str, ...]:
    """Return unique strings while preserving their first-seen order."""

    ordered: list[str] = []
    seen: set[str] = set()
    for value in values:
        if value in seen:
            continue
        ordered.append(value)
        seen.add(value)
    return tuple(ordered)


def priority_options() -> tuple[str, ...]:
    """Return Project priority options in display order."""

    return unique_values(PRIORITY_ALIASES.values())


def status_options() -> tuple[str, ...]:
    """Return Project status options in display order."""

    return unique_values(STATUS_ALIASES.values())


def managed_label_names() -> tuple[str, ...]:
    """Return managed issue-label names in display order."""

    return tuple(entry[0] for entry in MANAGED_KIND_LABELS.values())
