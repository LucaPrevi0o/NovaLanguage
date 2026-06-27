# Project automation

Nova uses GitHub Issues and the **Nova - Development Roadmap** GitHub Project to track roadmap work.

The automation layer is intentionally split by responsibility but now has a single Project workflow entry point:

```text
.github/workflows/project-automation.yml
```

That workflow coordinates validation, issue-to-Project synchronization, pull-request synchronization, roadmap drift checks, and manual repair commands. The Python scripts under `.github/scripts/` stay as small support commands invoked by workflow jobs instead of acting as a second workflow layer.

## Required secret

Jobs that write to the user-level GitHub Project need this repository secret:

```text
PROJECT_TOKEN
```

The token must have GitHub Projects write access, including the `project` scope for classic personal access tokens. The built-in `GITHUB_TOKEN` is still passed to scripts for normal repository access, but it is usually not enough for writing user-level Projects v2 fields.

## Issue forms and metadata

New issues should be opened through the YAML issue forms in `.github/ISSUE_TEMPLATE/`. Blank issues are disabled so contributors are guided through the same structured fields each time.

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

The issue sync job writes `Priority`, `Size`, and `Suggested status` to Project fields. The label check job validates native GitHub labels and milestones through `.github/scripts/issue_metadata.py`.

The legacy custom Project `Phase` field has been removed from the roadmap Project. The cleanup command remains available as an idempotent maintenance helper:

```bash
python3 .github/scripts/project_automation.py remove-legacy-phase-field --confirm
```

The legacy custom Project `Kind` field is also deprecated because labels are the source of truth. Remove it with:

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

## Consolidated workflow jobs

### Automation health

The `automation-health` job runs on pull requests and manual dispatches. It checks workflow/script wiring, issue-form shared fields, native metadata helper behavior, and Python syntax for the Project automation helpers.

### Native issue metadata check

The `check-issue-metadata` job runs for issue events other than `closed`.

Rules:

- every issue should have at least one managed label: `bug`, `feature`, `refactor`, `design`, `research`, `test`, or `docs`;
- every issue should have one managed roadmap milestone;
- unmanaged milestones fail the workflow so they are fixed before roadmap automation relies on them.

### Issue to Project sync

The `sync-issue` job runs when an issue is opened, edited, reopened, or transferred.

It coordinates the previous independent issue workflows in one job:

```bash
python3 .github/scripts/project_automation.py sync-issue --repo LucaPrevi0o/NovaLanguage --issue-number 28
python3 .github/scripts/project_automation.py sync-labels --repo LucaPrevi0o/NovaLanguage --issue-number 28
python3 .github/scripts/project_schedule.py --repo LucaPrevi0o/NovaLanguage --issue-number 28
```

Responsibilities:

- add the issue to the roadmap Project when missing;
- sync `Priority`, `Size`, and `Suggested status` into Project fields;
- preserve managed native labels unless legacy kind metadata explicitly asks for label migration;
- sync optional `Expected start` and `Expected deadline` into Roadmap date fields;
- ensure reopened issues are visible in the Project.

### Issue archive sync

The `sync-issue-archive` job runs when an issue is closed or reopened.

```bash
python3 .github/scripts/project_automation.py sync-issue-archive --repo LucaPrevi0o/NovaLanguage --issue-number 39
```

Rules:

- closed issues are archived only when their roadmap Project status is already `Done`;
- closed issues with another status produce a warning and remain visible;
- reopened/open issues are unarchived when they already exist in the Project;
- open issues missing from the Project are added back as visible items;
- the job does not change issue status, reopen issues, close issues, or edit issue metadata.

### Pull request metadata alignment

The `align-pr-metadata` job runs for non-closed pull request events.

```bash
python3 .github/scripts/pr_metadata_alignment.py --repo LucaPrevi0o/NovaLanguage --pr-number 12
```

It copies managed labels, a shared milestone, and selected Project fields from referenced issues to the pull request when the referenced issues agree.

### Pull request status sync

The `sync-pr-status` job runs for pull request events after the PR metadata alignment job has had a chance to run.

```bash
python3 .github/scripts/project_automation.py sync-pr-status --repo LucaPrevi0o/NovaLanguage --pr-number 12
```

Rules:

- draft pull requests do not change issue status;
- non-draft pull requests move referenced issues to `In Review`;
- merged pull requests move only closing-keyword issue references to `Done`;
- the job updates Project status only and does not close issues directly;
- after a merged closing-keyword PR moves an issue to `Done`, the script attempts the issue archive sync rule described above.

### Roadmap drift check

The `check-roadmap-drift` job runs on pull requests.

```bash
python3 .github/scripts/project_automation.py check-plan-drift --repo LucaPrevi0o/NovaLanguage --plan PLAN.md --readme README.md
```

Checks:

- `PLAN.md` and `README.md` should agree on the current focus phase;
- the current focus deliverable milestone should have at least one active Project item;
- the current focus deliverable milestone should normally have an `In Progress` Project item;
- shared deliverable milestones such as `Nova MVP compiler` are not used to infer whether one internal phase has stale work from another phase;
- immediate next steps are loosely matched to open issues and reported as notices.

Errors fail the workflow. Warnings are reported as GitHub annotations and can optionally be treated as failures through manual dispatch.

## Manual dispatch targets

The consolidated workflow exposes one `target` input:

- `health` runs local no-network automation checks;
- `issue` runs issue sync, label preservation, schedule sync, and archive visibility sync for one issue;
- `pr` runs PR metadata alignment and PR status sync for one pull request;
- `all-open` repairs Project fields, labels, schedules, and archive visibility for every open issue;
- `all-closed` repairs archive state for every closed issue;
- `drift` runs the roadmap drift check.

Bulk repair commands remain available locally:

```bash
python3 .github/scripts/project_automation.py sync-issue --repo LucaPrevi0o/NovaLanguage --all-open
python3 .github/scripts/project_automation.py sync-labels --repo LucaPrevi0o/NovaLanguage --all-open
python3 .github/scripts/project_schedule.py --repo LucaPrevi0o/NovaLanguage --all-open
python3 .github/scripts/project_automation.py sync-issue-archive --repo LucaPrevi0o/NovaLanguage --all-closed
python3 .github/scripts/project_automation.py sync-issue-archive --repo LucaPrevi0o/NovaLanguage --all-open
```
