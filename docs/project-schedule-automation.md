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

Schedule synchronization now runs through the consolidated Project automation workflow:

```text
.github/workflows/project-automation.yml
```

The script entry point remains:

```text
.github/scripts/project_schedule.py
```

The workflow runs schedule sync when an issue is opened, edited, reopened, or transferred. It can also be run manually through the `issue` target for one issue or the `all-open` target for every open issue.

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

Legacy schedule values in `## Project metadata` are not synchronized. Use the legacy metadata audit to find old issue bodies that still need to be migrated to current `### Expected start` and `### Expected deadline` headings:

```bash
python3 .github/scripts/project_automation.py audit-legacy-metadata --repo LucaPrevi0o/NovaLanguage --all-open
```
