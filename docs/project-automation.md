# Project automation

Nova uses GitHub Issues and the **Nova - Development Roadmap** GitHub Project to track roadmap work.

The repository also contains GitHub Actions workflows that keep that project metadata aligned with issue bodies and pull request activity.

## Required secret

Workflows that write to the user-level GitHub Project need a repository secret:

```text
PROJECT_TOKEN
```

The token must have GitHub Projects write access, including the `project` scope for classic personal access tokens.

The built-in `GITHUB_TOKEN` is still passed to the scripts for normal repository access, but it is usually not enough for writing user-level Projects v2 fields.

## Issue metadata block

Project sync reads this block from issue bodies:

```markdown
## Project metadata

- Phase: Phase 5 - Type model
- Area: type-system / semantic-analysis
- Kind: refactor
- Priority: P1
- Size: M
- Suggested status: Ready
```

Supported synced fields:

- `Phase`
- `Kind`
- `Priority`
- `Size`
- `Suggested status`

The workflow maps short issue values such as `P1` into Project values such as `1 - Important next step`.

When an issue contains invalid metadata, single-issue runs fail loudly. Bulk sync runs warn and continue so one old issue does not block the whole resync.

## Issue to Project sync

Workflow: `.github/workflows/project-sync.yml`

Script command:

```bash
python3 .github/scripts/project_automation.py sync-issue --repo LucaPrevi0o/NovaLanguage --issue-number 28 --strict
```

Triggers:

- issue opened;
- issue edited;
- issue reopened;
- issue transferred;
- manual dispatch for one issue;
- manual dispatch for every open issue.

Responsibilities:

- add the issue to the roadmap Project when missing;
- parse the issue `Project metadata` block;
- sync valid metadata into Project fields;
- report missing or invalid metadata clearly.

## Planned workflow automation

Additional workflows are tracked under issue #27:

- issue label synchronization;
- `PLAN.md` and Project drift checks.

## Pull request status sync

Workflow: `.github/workflows/project-pr-status.yml`

Script command:

```bash
python3 .github/scripts/project_automation.py sync-pr-status --repo LucaPrevi0o/NovaLanguage --pr-number 12
```

Triggers:

- pull request opened;
- pull request edited;
- pull request reopened;
- pull request marked ready for review;
- pull request converted to draft;
- pull request closed;
- manual dispatch for one pull request.

Rules:

- draft pull requests do not change issue status;
- non-draft pull requests move referenced issues to `In Review`;
- merged pull requests move only closing-keyword issue references to `Done`;
- the workflow updates Project status only and does not close issues directly.
