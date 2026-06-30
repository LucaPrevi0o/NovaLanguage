# Automation health check

Nova's GitHub automation is split between YAML workflows and a Python package under `.github/scripts/nova_automation`.
The automation health check is a no-network guard for safe refactors of that layer.

It is intentionally behavior-preserving: it does not call the GitHub REST API, the GitHub GraphQL API, or the roadmap Project.
Instead, it verifies that the repository wiring still makes sense before more invasive automation refactors are attempted.

## Entry points

Workflow:

```text
.github/workflows/project-automation.yml
```

Package module:

```text
nova_automation.cli.automation_health
```

Local command:

```bash
PYTHONPATH=.github/scripts python3 -m nova_automation.cli.automation_health
```

## What it checks

The health check currently verifies that:

- expected automation workflows still exist;
- expected Python automation package files still exist;
- every Python file under `.github/scripts/` compiles without being imported;
- workflows do not reference removed `.github/scripts/*.py` compatibility wrappers;
- workflows only call subcommands declared by `nova_automation.cli.project_automation`;
- workflow-managed project commands remain wired from at least one workflow;
- the consolidated Project automation workflow and this health-check module are documented.

The consolidated workflow also runs the existing issue-form checks:

```bash
PYTHONPATH=.github/scripts python3 -m nova_automation.issues.forms --check
PYTHONPATH=.github/scripts python3 -m nova_automation.issues.native_metadata --issue-number 0 --labels refactor --milestone "Nova MVP compiler"
```

It also runs the lightweight Python unit tests for automation helper behavior:

```bash
PYTHONPATH=.github/scripts python3 -m unittest discover -s .github/tests -p "*_test.py"
```

## Scope

This check protects the YAML/Python automation layer from accidental drift while preserving the externally visible behavior of existing workflows.
It is a safety net for follow-up work such as further splitting automation modules, reducing duplicated workflow dispatch logic, or removing obsolete migration helpers.

It does not replace repository tests. Run the Java test suite separately when validating the whole project:

```bash
./mvnw test
```

It also does not validate secret permissions such as `PROJECT_TOKEN`; workflows that write to the GitHub Project still validate those permissions only when they run with real GitHub credentials.
