"""Pull request issue-reference parsing helpers."""

from __future__ import annotations

import re

from ..github import repository_parts


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
