# Project automation

Nova uses GitHub Issues and the **Nova - Development Roadmap** GitHub Project to track roadmap work.

The repository also contains GitHub Actions workflows that keep that project metadata aligned with issue bodies, native issue metadata, and pull request activity.

## Required secret

Workflows that write to the user-level GitHub Project need a repository secret:

```text
PROJECT_TOKEN
```

The token must have GitHub Projects write access, including the `project` scope for classic personal access tokens.

The built-in `GITHUB_TOKEN` is still passed to the scripts for normal repository access, but it is usually not enough for writing user-level Projects v2 fields.

## Issue forms and metadata

New issues should be opened through the YAML issue forms in `.github/ISSUE_TEMPLATE/`.
Blank issues are disabled so contributors are guided through the same structured fields each time.

Issue forms do not duplicate native GitHub issue metadata in the body:

- GitHub issue labels are the source of truth for issue kind.
- GitHub issue milestones are the source of truth for roadmap grouping.

The forms only collect metadata that GitHub does not natively model for this repository workflow:

```markdown
### Priority

1 - Important next step

### Size

M

### Suggested status

Ready
```

Existing issues that still contain older body metadata remain supported during migration:

```markdown
## Project metadata

- Milestone: Nova MVP compiler
- Area: type-system / semantic-analysis
- Kind: refactor
- Priority: P1
- Size: M
- Suggested status: Ready
```

Supported legacy/body metadata keys:

- `Milestone`
- `Kind`
- `Priority`
- `Size`
- `Suggested status`

The issue sync workflow writes `Priority`, `Size`, and `Suggested status` to Project fields. Legacy `Milestone` or `Phase` metadata is still accepted during migration, but new issues should use the native GitHub milestone selector instead of a form body field.

The label sync workflow still understands legacy `Kind` metadata. New issues should use native GitHub labels instead. If an issue has no legacy `Kind` metadata, label sync keeps existing managed labels instead of treating missing body metadata as an error.

The legacy custom Project `Phase` field has been removed from the roadmap Project. The cleanup command remains available as an idempotent maintenance helper:

```bash
python3 .github/scripts/project_automation.py remove-legacy-phase-field --confirm
```

The legacy custom Project `Kind` field is also deprecated because labels are the source of truth. After label-only automation is active on the default branch, remove it with:

```bash
python3 .github/scripts/project_automation.py remove-legacy-kind-field --confirm
```

Managed milestone names:

- `Project workflow`
- `Nova MVP compiler`
- `Advanced overload and override rules`
- `Access control`
- `Inheritance conflict checks`
- `Generics`
- `Bounded generics`
- `Class parameters`
- `Operator-overloadable Nova types`
- `Lambdas`
- `Variadic generics`
- `Monomorphization`
- `Future development`

Use `Nova MVP compiler` for Phase 1 through Phase 8 work: build health, parser stability, diagnostics, semantic separation, the type model, the multi-file pipeline, standard-library loading, and IR preparation. Use the specific advanced-feature milestones for post-MVP Phase 9 work.

The workflow maps short issue values such as `P1` into Project values such as `1 - Important next step` for legacy issues.

When an issue contains invalid metadata, single-issue runs fail loudly. Bulk sync runs warn and continue so one old issue does not block the whole resync.

## Issue metadata check

Workflow: `.github/workflows/issue-metadata-check.yml`

Triggers:

- issue opened;
- issue edited;
- issue reopened;
- issue transferred.

Rules:

- every issue should have at least one managed label: `bug`, `feature`, `refactor`, `design`, `research`, `test`, or `docs`;
- every issue should have one managed roadmap milestone;
- unmanaged milestones fail the workflow so they are fixed before roadmap automation relies on them.

This workflow validates native GitHub issue properties through `.github/scripts/issue_metadata.py`, which imports the managed labels and milestones from `.github/scripts/project_metadata.py`. It does not edit labels, milestones, issue bodies, or Project fields.

## Issue to Project sync

Workflow: `.github/workflows/project-sync.yml`

Script command:

```bash
python3 .github/scripts/project_automation.py sync-issue --repo LucaPrevi0o/NovaLanguage --issue-number 28
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
- parse Project metadata from issue-form sections or the legacy `Project metadata` block;
- sync legacy body `Milestone` or `Phase` metadata into the issue milestone when present;
- sync `Priority`, `Size`, and `Suggested status` into Project fields;
- ignore the removed custom Project `Phase` field while still accepting legacy `Phase` metadata in issue bodies;
- ignore the deprecated custom Project `Kind` field because issue labels now represent kind metadata;
- warn about missing legacy milestone metadata without failing new issues that rely on native labels and milestones;
- fail when `Priority`, `Size`, or `Suggested status` maps to an option that does not exist in the roadmap Project.

## Workflow automation roadmap

The current workflow automation set was coordinated under issue #36. New automation should be introduced through focused follow-up issues before it is added to this script.

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
- the workflow updates Project status only and does not close issues directly;
- after a merged closing-keyword PR moves an issue to `Done`, the same script attempts the issue archive sync rule described below.

## Issue archive sync

Workflow: `.github/workflows/project-archive-sync.yml`

Script command:

```bash
python3 .github/scripts/project_automation.py sync-issue-archive --repo LucaPrevi0o/NovaLanguage --issue-number 39
```

Bulk repair commands:

```bash
python3 .github/scripts/project_automation.py sync-issue-archive --repo LucaPrevi0o/NovaLanguage --all-closed
python3 .github/scripts/project_automation.py sync-issue-archive --repo LucaPrevi0o/NovaLanguage --all-open
```

Triggers:

- issue closed;
- issue reopened;
- manual dispatch for one issue;
- manual dispatch for every closed issue;
- manual dispatch for every open issue.

Rules:

- closed issues are archived only when their roadmap Project status is already `Done`;
- closed issues with another status produce a warning and remain visible;
- reopened/open issues are unarchived when they already exist in the Project;
- open issues missing from the Project are added back as visible items;
- the workflow does not change issue status, reopen issues, close issues, or edit issue metadata.

## Issue label sync

Workflow: `.github/workflows/project-label-sync.yml`

Script command:

```bash
python3 .github/scripts/project_automation.py sync-labels --repo LucaPrevi0o/NovaLanguage --issue-number 30
```

Triggers:

- issue opened;
- issue edited;
- issue reopened;
- issue transferred;
- manual dispatch for one issue;
- manual dispatch for every open issue.

The workflow syncs only labels owned by legacy `Kind` metadata when that metadata exists:

- `bug`
- `feature`
- `refactor`
- `design`
- `research`
- `test`
- `docs`

It may remove stale labels from that managed set when an issue's legacy kind metadata changes, but it does not remove unrelated labels that were added manually. New issue forms do not include `Kind`/`Labels` body metadata, so native issue labels should be managed directly through GitHub and checked by the `Check issue metadata` workflow.

## Roadmap drift check

Workflow: `.github/workflows/project-drift-check.yml`

Script command:

```bash
python3 .github/scripts/project_automation.py check-plan-drift --repo LucaPrevi0o/NovaLanguage --plan PLAN.md --readme README.md
```

Triggers:

- pull requests that change `PLAN.md`;
- pull requests that change `README.md`;
- pull requests that change Markdown docs;
- pull requests that change Project automation scripts or workflows;
- manual dispatch.

Checks:

- `PLAN.md` and `README.md` should agree on the current focus phase;
- the current focus deliverable milestone should have at least one active Project item;
- the current focus deliverable milestone should normally have an `In Progress` Project item;
- shared deliverable milestones such as `Nova MVP compiler` are not used to infer whether one internal phase has stale work from another phase;
- immediate next steps are loosely matched to open issues and reported as notices.

Errors fail the workflow. Warnings are reported as GitHub annotations and can optionally be treated as failures through manual dispatch.
