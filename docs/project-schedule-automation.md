# Project schedule automation

Nova issue forms can provide optional schedule metadata for GitHub Projects Roadmap planning.

## Issue fields

Every issue form includes two optional date fields:

- `Expected start`
- `Expected deadline`

Both fields use ISO date format:

```text
YYYY-MM-DD
```

Example:

```markdown
### Expected start

2026-07-01

### Expected deadline

2026-07-31
```

Empty values are ignored. They do not clear Project dates that were already set manually or by an earlier workflow run.

## Workflow

Workflow:

```text
.github/workflows/project-schedule-sync.yml
```

Script:

```text
.github/scripts/project_schedule.py
```

The workflow runs when an issue is opened, edited, reopened, or transferred. It can also be run manually for one issue or for every open issue.

## Project fields

By default, the script writes issue schedule metadata to these Project date fields:

- `Start date`
- `End date`

The field names can be overridden in the workflow environment:

```yaml
PROJECT_START_DATE_FIELD: Start date
PROJECT_END_DATE_FIELD: End date
```

The script expects those date fields to already exist in the configured roadmap Project. It does not create Project fields automatically.

## Local commands

Sync one issue:

```bash
python3 .github/scripts/project_schedule.py --repo LucaPrevi0o/NovaLanguage --issue-number 54
```

Sync every open issue that has schedule metadata:

```bash
python3 .github/scripts/project_schedule.py --repo LucaPrevi0o/NovaLanguage --all-open
```

The script validates date format before writing to the Project. If both dates are present, `Expected start` must not be later than `Expected deadline`.

## Legacy metadata

The script also accepts schedule values in the older `## Project metadata` block while old issues are being migrated:

```markdown
## Project metadata

- Expected start: 2026-07-01
- Expected deadline: 2026-07-31
```
