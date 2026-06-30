# Issue and pull request templates

Nova uses structured GitHub issue forms to keep roadmap metadata consistent and avoid free-form issue clutter.

## Available issue forms

- **Bug report**: regressions, diagnostics, or compiler/front-end behavior.
- **Compiler task**: focused implementation, test, documentation, or cleanup work.
- **Design task**: decisions about language behavior, compiler architecture, workflow, or roadmap direction.
- **Feature proposal**: proposed Nova language or compiler capabilities before implementation starts.
- **Refactoring task**: behavior-preserving code, test, workflow, or documentation restructuring.

Blank issues are disabled in `.github/ISSUE_TEMPLATE/config.yml`, so contributors are guided through these templates.

## Native GitHub metadata

Issue forms intentionally do **not** ask for `Labels` or `Milestone` in the issue body. Those are native GitHub issue properties and should be set through GitHub's label and milestone controls.

The native source of truth is:

- GitHub issue labels for work kind, such as `bug`, `feature`, `refactor`, `design`, `research`, `test`, or `docs`.
- GitHub issue milestones for roadmap grouping, such as `Nova MVP compiler`, `Project workflow`, advanced feature milestones, or `Future development`.

The `Check issue metadata` workflow validates new or edited issues and fails when an issue has no managed label, no milestone, or an unmanaged milestone.

Milestone due dates can be used as planning guidance when choosing issue-level schedule fields, but the issue forms still keep milestone selection itself in native GitHub metadata.

## Issue-form metadata

Every issue form still uses these shared project fields because GitHub does not provide native equivalents for this repository workflow:

- `Priority`
- `Size`
- `Suggested status`
- `Expected start`
- `Expected deadline`

`Priority`, `Size`, and `Suggested status` are parsed by the project automation documented in `docs/project-automation.md` and synchronized into the roadmap Project.

`Expected start` and `Expected deadline` are optional `YYYY-MM-DD` fields. When present, `nova_automation.project.schedule` writes them into the roadmap Project date fields used by the Roadmap view. Empty schedule fields are ignored and do not clear existing Project dates.

Legacy issue bodies that still contain `### Milestone`, `### Labels`, or a `## Project metadata` block are not a supported source for Project synchronization. New issue forms should not reintroduce those duplicated body fields, and existing issues should keep native labels, native milestones, and the current `Priority`, `Size`, `Suggested status`, `Expected start`, and `Expected deadline` headings instead.

Run the legacy metadata audit before changing automation that touches issue metadata, and migrate any open findings to native labels, native milestones, and current issue-form fields:

```bash
PYTHONPATH=.github/scripts python3 -m nova_automation.cli.project_automation audit-legacy-metadata --repo LucaPrevi0o/NovaLanguage --all-open
```

## Keeping form options aligned

The canonical source for managed labels, milestones, priorities, statuses, and sizes is `nova_automation.project.metadata`. The helper module `nova_automation.issues.forms` imports those constants, updates the remaining shared option blocks, and rejects duplicated native body fields. The native issue metadata check also imports the same constants through `nova_automation.issues.native_metadata`, so label and milestone validation follows the same source of truth.

To update forms locally after changing managed metadata:

```bash
PYTHONPATH=.github/scripts python3 -m nova_automation.issues.forms
```

To check that committed forms are aligned without editing files:

```bash
PYTHONPATH=.github/scripts python3 -m nova_automation.issues.forms --check
```

The `Check issue forms` workflow runs the check mode on pull requests that touch issue forms or the related automation scripts.

## Pull request template

The pull request template is intentionally lighter than issue forms. It asks authors to either link a tracking issue or mark the PR as self-contained, which keeps small implementation PRs from requiring extra issues while still preserving issue-to-Project automation when an issue exists.
