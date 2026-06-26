# Automation health check

Nova's GitHub automation is split across YAML workflows and Python scripts under `.github/`.
The automation health check is a no-network guard for safe refactors of that layer.

It is intentionally behavior-preserving: it does not call the GitHub REST API, the GitHub GraphQL API, or the roadmap Project.
Instead, it verifies that the repository wiring still makes sense before more invasive automation refactors are attempted.

## Entry points

Workflow:

```text
.github/workflows/automation-health-check.yml
```

Script:

```text
.github/scripts/automation_health.py
```

Local command:

```bash
python3 .github/scripts/automation_health.py
```

## What it checks

The script currently checks that:

- expected automation workflows still exist;
- expected Python automation scripts still exist;
- every Python script under `.github/scripts/` compiles without being imported;
- workflow references such as `python3 .github/scripts/project_automation.py` point to existing files;
- workflows only call subcommands declared by `project_automation.py`;
- workflow-managed project commands remain wired from at least one workflow;
- this health-check workflow and script are documented.

The workflow also runs the existing issue-form checks:

```bash
python3 .github/scripts/issue_forms.py --check
python3 .github/scripts/issue_metadata.py --issue-number 0 --labels refactor --milestone "Nova MVP compiler"
```

## Scope

This check protects the YAML/Python automation layer from accidental drift while preserving the externally visible behavior of existing workflows.
It is a safety net for follow-up work such as splitting `project_automation.py`, reducing duplicated workflow dispatch logic, or removing obsolete migration helpers.

It does not replace repository tests. Run the Java test suite separately when validating the whole project:

```bash
./mvnw test
```

It also does not validate secret permissions such as `PROJECT_TOKEN`; workflows that write to the GitHub Project still validate those permissions only when they run with real GitHub credentials.
