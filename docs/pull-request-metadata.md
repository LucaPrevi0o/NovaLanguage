# Pull request metadata

Nova uses issue metadata to organize work in the roadmap Project. A pull request that references an issue can copy a small set of that metadata so review work is visible too.

Workflow: `.github/workflows/project-automation.yml`

Package module: `nova_automation.pull_requests.metadata_alignment`

Local command:

```bash
PYTHONPATH=.github/scripts python3 -m nova_automation.pull_requests.metadata_alignment --repo LucaPrevi0o/NovaLanguage --pr-number 12
```

Behavior:

- add managed issue labels to the pull request;
- keep existing pull request labels;
- copy the milestone only when referenced issues agree;
- add the pull request to the roadmap Project;
- copy `Priority` and `Size` when referenced issues agree;
- set ready pull requests to `In Review` in the Project;
- leave draft pull request status unchanged.

Conflicts are reported as warnings. The workflow does not edit source issues.
