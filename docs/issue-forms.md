# Issue and pull request templates

Nova uses structured GitHub issue forms to keep roadmap metadata consistent and avoid free-form issue clutter.

## Available issue forms

- **Bug report**: regressions, incorrect diagnostics, or wrong compiler/front-end behavior.
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

## Issue-form metadata

Every issue form still uses these shared project fields because GitHub does not provide native equivalents for this repository workflow:

- `Priority`
- `Size`
- `Suggested status`

Those fields are parsed by the project automation documented in `docs/project-automation.md` and synchronized into the roadmap Project.

Legacy issue bodies that still contain `### Milestone`, `### Labels`, or a `## Project metadata` block remain supported by the existing automation during migration, but new issue forms should not reintroduce those duplicated body fields.

## Keeping form options aligned

The canonical source for managed labels, milestones, priorities, statuses, and sizes is `.github/scripts/project_metadata.py`. The helper script `.github/scripts/issue_forms.py` imports those constants, updates the remaining shared option blocks, and rejects duplicated native body fields.

To update forms locally after changing managed metadata:

```bash
python3 .github/scripts/issue_forms.py
```

To check that committed forms are aligned without editing files:

```bash
python3 .github/scripts/issue_forms.py --check
```

The `Check issue forms` workflow runs the check mode on pull requests that touch issue forms or the related automation scripts.

## Pull request template

The pull request template is intentionally lighter than issue forms. It asks authors to either link a tracking issue or mark the PR as self-contained, which keeps small implementation PRs from requiring extra issues while still preserving issue-to-Project automation when an issue exists.
