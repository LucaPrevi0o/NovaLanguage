#!/usr/bin/env python3
"""Validate GitHub automation wiring without calling GitHub APIs."""

from __future__ import annotations

import argparse
import py_compile
import re
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path

EXPECTED_SCRIPTS = (
    ".github/scripts/automation_health.py",
    ".github/scripts/issue_forms.py",
    ".github/scripts/issue_metadata.py",
    ".github/scripts/mirror_docs_to_wiki.py",
    ".github/scripts/pr_metadata_alignment.py",
    ".github/scripts/prepare_javadoc_site.py",
    ".github/scripts/project_automation.py",
    ".github/scripts/project_board.py",
    ".github/scripts/project_github.py",
    ".github/scripts/project_issue_metadata.py",
    ".github/scripts/project_metadata.py",
    ".github/scripts/project_schedule.py",
    ".github/scripts/nova_automation/__init__.py",
    ".github/scripts/nova_automation/cli/__init__.py",
    ".github/scripts/nova_automation/cli/automation_health.py",
    ".github/scripts/nova_automation/cli/project_automation.py",
    ".github/scripts/nova_automation/docs/__init__.py",
    ".github/scripts/nova_automation/docs/pages.py",
    ".github/scripts/nova_automation/docs/wiki.py",
    ".github/scripts/nova_automation/github/__init__.py",
    ".github/scripts/nova_automation/github/client.py",
    ".github/scripts/nova_automation/github/models.py",
    ".github/scripts/nova_automation/github/repository.py",
    ".github/scripts/nova_automation/issues/__init__.py",
    ".github/scripts/nova_automation/issues/forms.py",
    ".github/scripts/nova_automation/issues/labels.py",
    ".github/scripts/nova_automation/issues/native_metadata.py",
    ".github/scripts/nova_automation/project/__init__.py",
    ".github/scripts/nova_automation/project/board.py",
    ".github/scripts/nova_automation/project/issue_metadata.py",
    ".github/scripts/nova_automation/project/metadata.py",
    ".github/scripts/nova_automation/project/schedule.py",
    ".github/scripts/nova_automation/pull_requests/__init__.py",
    ".github/scripts/nova_automation/pull_requests/metadata_alignment.py",
    ".github/scripts/nova_automation/pull_requests/references.py",
    ".github/scripts/nova_automation/pull_requests/status.py",
    ".github/scripts/nova_automation/roadmap/__init__.py",
    ".github/scripts/nova_automation/roadmap/drift.py",
)

EXPECTED_WORKFLOWS = (
    ".github/workflows/docs-publish.yml",
    ".github/workflows/java-ci.yml",
    ".github/workflows/project-automation.yml",
    ".github/workflows/qodana_code_quality.yml",
)

PROJECT_AUTOMATION_COMMANDS = (
    "check-plan-drift",
    "sync-issue",
    "sync-issue-archive",
    "sync-labels",
    "sync-pr-status",
)

WORKFLOW_MANAGED_PROJECT_COMMANDS = PROJECT_AUTOMATION_COMMANDS
DOCUMENTED_ENTRY_POINTS = (
    ".github/scripts/automation_health.py",
    ".github/workflows/project-automation.yml",
)

SCRIPT_REFERENCE_PATTERN = re.compile(r"(?<![\w/.-])\.github/scripts/[A-Za-z0-9_./-]+\.py")
PROJECT_COMMAND_PATTERN = re.compile(r"\.github/scripts/project_automation\.py\s+([a-z][a-z0-9-]+)")
ADD_PARSER_PATTERN = re.compile(r"add_parser\(\s*[\"']([^\"']+)[\"']")


@dataclass(frozen=True)
class Finding:
    """One automation-health finding."""

    level: str
    message: str
    path: Path | None = None


class AutomationHealthError(RuntimeError):
    """Raised when the automation health check cannot inspect the repository."""


def default_root() -> Path:
    """Return the repository root when this module lives under nova_automation/cli."""

    return Path(__file__).resolve().parents[4]


def annotation_value(value: str) -> str:
    """Escape text for GitHub Actions annotation commands."""

    return value.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def relative_path(path: Path, root: Path) -> str:
    """Return a stable repository-relative display path."""

    try:
        return path.resolve().relative_to(root.resolve()).as_posix()
    except ValueError:
        return path.as_posix()


def print_finding(finding: Finding, root: Path) -> None:
    """Print one GitHub Actions-compatible annotation."""

    level = finding.level
    message = annotation_value(finding.message)
    if finding.path is None:
        print(f"::{level}::{message}")
        return
    path = annotation_value(relative_path(finding.path, root))
    print(f"::{level} file={path}::{message}")


def require_file(root: Path, relative: str, findings: list[Finding]) -> Path:
    """Check that a repository-relative file exists."""

    path = root / relative
    if not path.is_file():
        findings.append(Finding("error", f"Expected automation file is missing: {relative}", path))
    return path


def read_text(path: Path) -> str:
    """Read UTF-8 text and add context to decoding failures."""

    try:
        return path.read_text(encoding="utf-8")
    except OSError as error:
        raise AutomationHealthError(f"Could not read {path}: {error}") from error


def workflow_paths(root: Path) -> list[Path]:
    """Return all workflow YAML files."""

    workflow_dir = root / ".github" / "workflows"
    if not workflow_dir.is_dir():
        raise AutomationHealthError(".github/workflows directory is missing")
    return sorted(path for path in workflow_dir.iterdir() if path.suffix in {".yml", ".yaml"})


def script_paths(root: Path) -> list[Path]:
    """Return all Python automation scripts."""

    script_dir = root / ".github" / "scripts"
    if not script_dir.is_dir():
        raise AutomationHealthError(".github/scripts directory is missing")
    return sorted(script_dir.glob("**/*.py"))


def check_expected_files(root: Path, findings: list[Finding]) -> None:
    """Check that the known automation entry points still exist."""

    for relative in (*EXPECTED_SCRIPTS, *EXPECTED_WORKFLOWS):
        require_file(root, relative, findings)


def compile_python_scripts(root: Path, findings: list[Finding]) -> None:
    """Compile Python scripts to catch syntax errors without importing them."""

    with tempfile.TemporaryDirectory(prefix="nova-automation-pyc-") as bytecode_dir:
        bytecode_root = Path(bytecode_dir)
        for path in script_paths(root):
            relative = path.relative_to(root)
            cfile = bytecode_root / relative.with_suffix(".pyc")
            cfile.parent.mkdir(parents=True, exist_ok=True)
            try:
                py_compile.compile(str(path), cfile=str(cfile), doraise=True)
            except py_compile.PyCompileError as error:
                findings.append(Finding("error", f"Python syntax check failed: {error.msg}", path))


def referenced_scripts_by_workflow(root: Path) -> dict[Path, set[str]]:
    """Return Python script paths referenced by each workflow file."""

    references: dict[Path, set[str]] = {}
    for workflow in workflow_paths(root):
        text = read_text(workflow)
        references[workflow] = set(SCRIPT_REFERENCE_PATTERN.findall(text))
    return references


def declared_project_commands(root: Path, findings: list[Finding]) -> set[str]:
    """Return subcommands declared by project_automation.py."""

    path = root / ".github" / "scripts" / "nova_automation" / "cli" / "project_automation.py"
    if not path.is_file():
        findings.append(Finding("error", "nova_automation/cli/project_automation.py is missing", path))
        return set()
    return set(ADD_PARSER_PATTERN.findall(read_text(path)))


def workflow_project_commands(root: Path) -> dict[Path, set[str]]:
    """Return project_automation.py subcommands invoked by workflows."""

    commands: dict[Path, set[str]] = {}
    for workflow in workflow_paths(root):
        text = read_text(workflow)
        normalized = re.sub(r"\\\s*\n\s*", " ", text)
        commands[workflow] = set(PROJECT_COMMAND_PATTERN.findall(normalized))
    return commands


def check_workflow_script_references(root: Path, findings: list[Finding]) -> None:
    """Check that workflow Python script references point to existing files."""

    for workflow, references in referenced_scripts_by_workflow(root).items():
        for reference in sorted(references):
            if not (root / reference).is_file():
                findings.append(Finding("error", f"Workflow references missing script {reference}", workflow))


def check_project_command_wiring(root: Path, findings: list[Finding]) -> None:
    """Check project_automation.py subcommands and workflow references."""

    project_script = root / ".github" / "scripts" / "nova_automation" / "cli" / "project_automation.py"
    declared = declared_project_commands(root, findings)
    missing_commands = sorted(set(PROJECT_AUTOMATION_COMMANDS) - declared)
    for command in missing_commands:
        findings.append(Finding("error", f"Expected project automation subcommand is missing: {command}", project_script))

    workflow_commands = workflow_project_commands(root)
    invoked = set().union(*workflow_commands.values()) if workflow_commands else set()

    unknown_commands = sorted(invoked - declared)
    for command in unknown_commands:
        workflows = ", ".join(relative_path(path, root) for path, commands in workflow_commands.items() if command in commands)
        findings.append(Finding("error", f"Workflow invokes unknown project automation command '{command}' in {workflows}"))

    for command in sorted(set(WORKFLOW_MANAGED_PROJECT_COMMANDS) - invoked):
        findings.append(Finding("error", f"No workflow invokes project automation command: {command}", project_script))


def documented_automation_text(root: Path) -> str:
    """Return combined docs text used for lightweight documentation checks."""

    docs_dir = root / "docs"
    if not docs_dir.is_dir():
        return ""
    return "\n".join(read_text(path) for path in sorted(docs_dir.glob("*.md")))


def check_documentation_mentions(root: Path, findings: list[Finding]) -> None:
    """Check that automation health-check entry points are documented."""

    docs_text = documented_automation_text(root)
    docs_path = root / "docs" / "automation-health-check.md"
    for expected in DOCUMENTED_ENTRY_POINTS:
        if expected not in docs_text:
            findings.append(Finding("error", f"Automation health-check docs do not mention {expected}", docs_path))


def run_health_check(root: Path) -> list[Finding]:
    """Run all no-network automation checks."""

    findings: list[Finding] = []
    check_expected_files(root, findings)
    compile_python_scripts(root, findings)
    check_workflow_script_references(root, findings)
    check_project_command_wiring(root, findings)
    check_documentation_mentions(root, findings)
    return findings


def build_parser() -> argparse.ArgumentParser:
    """Build the automation-health command-line parser."""

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=default_root(), help="Repository root path")
    return parser


def main() -> int:
    """Run the automation health check."""

    args = build_parser().parse_args()
    root = args.root.resolve()
    try:
        findings = run_health_check(root)
    except AutomationHealthError as error:
        print_finding(Finding("error", str(error)), root)
        return 1

    if not findings:
        print("Automation health check passed.")
        return 0

    for finding in findings:
        print_finding(finding, root)
    return 1 if any(finding.level == "error" for finding in findings) else 0


if __name__ == "__main__":
    sys.exit(main())
