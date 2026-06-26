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

- Milestone: Nova MVP compiler
- Area: type-system / semantic-analysis
- Kind: refactor
- Priority: P1
- Size: M
- Suggested status: Ready
```

Supported metadata keys:

- `Milestone`
- `Kind`
- `Priority`
- `Size`
- `Suggested status`

The issue sync workflow writes `Milestone` to the GitHub issue milestone and
writes `Priority`, `Size`, and `Suggested status` to Project fields. Legacy
`Phase` metadata is still accepted during migration, but Phase 1 through Phase 8
values now map to the shared `Nova MVP compiler` milestone instead of one
milestone per internal roadmap phase.

The label sync workflow writes `Kind` metadata to GitHub issue labels. `Kind`
may contain one value, such as `refactor`, or multiple values separated with
slashes, commas, or semicolons, such as `docs / design`.

The legacy custom Project `Phase` field has been removed from the roadmap
Project. The cleanup command remains available as an idempotent maintenance
helper:

```bash
python3 .github/scripts/project_automation.py remove-legacy-phase-field --confirm
```

The legacy custom Project `Kind` field is also deprecated because labels are the
source of truth. After label-only automation is active on the default branch,
remove it with:

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

Use `Nova MVP compiler` for Phase 1 through Phase 8 work: build health,
parser stability, diagnostics, semantic separation, the type model, the
multi-file pipeline, standard-library loading, and IR preparation. Use the
specific advanced-feature milestones for post-MVP Phase 9 work.

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
- sync roadmap grouping into the issue milestone;
- sync `Priority`, `Size`, and `Suggested status` into Project fields;
- ignore the removed custom Project `Phase` field while still accepting legacy
  `Phase` metadata in issue bodies;
- ignore the deprecated custom Project `Kind` field because issue labels now
  represent kind metadata;
- report missing or invalid metadata clearly.

## Planned workflow automation

Additional workflow automation is tracked under issue #36.

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

## Issue label sync

Workflow: `.github/workflows/project-label-sync.yml`

Script command:

```bash
python3 .github/scripts/project_automation.py sync-labels --repo LucaPrevi0o/NovaLanguage --issue-number 30 --strict
```

Triggers:

- issue opened;
- issue edited;
- issue reopened;
- issue transferred;
- manual dispatch for one issue;
- manual dispatch for every open issue.

The workflow syncs only labels owned by `Kind` metadata:

- `bug`
- `feature`
- `refactor`
- `design`
- `research`
- `test`
- `docs`

It may remove stale labels from that managed set when an issue's kind changes,
but it does not remove unrelated labels that were added manually. Multiple kind
values are supported in one metadata block, so `Kind: docs / design` keeps both
the `docs` and `design` labels on the issue.

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
- shared deliverable milestones such as `Nova MVP compiler` are not used to infer
  whether one internal phase has stale work from another phase;
- immediate next steps are loosely matched to open issues and reported as notices.

Errors fail the workflow. Warnings are reported as GitHub annotations and can optionally be treated as failures through manual dispatch.
