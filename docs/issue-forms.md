# Issue and pull request templates

Nova uses structured GitHub issue forms to keep roadmap metadata consistent and avoid free-form issue clutter.

## Available issue forms

- **Bug report**: regressions, incorrect diagnostics, or wrong compiler/front-end behavior.
- **Compiler task**: focused implementation, test, documentation, or cleanup work.
- **Design task**: decisions about language behavior, compiler architecture, workflow, or roadmap direction.
- **Feature proposal**: proposed Nova language or compiler capabilities before implementation starts.
- **Refactoring task**: behavior-preserving code, test, workflow, or documentation restructuring.

Blank issues are disabled in `.github/ISSUE_TEMPLATE/config.yml`, so contributors are guided through these templates.

## Shared metadata

Every issue form uses the same shared metadata fields:

- `Milestone`
- `Labels`
- `Priority`
- `Size`
- `Suggested status`

The metadata is parsed by the project automation documented in `docs/project-automation.md`. The `Labels` checkbox group is treated as `Kind` metadata and synchronized to the managed GitHub labels.

## Keeping form options aligned

The canonical source for managed milestones, labels, priorities, and statuses is `.github/scripts/project_automation.py`. The helper script `.github/scripts/issue_forms.py` imports those constants and updates the matching option blocks in every issue form.

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
