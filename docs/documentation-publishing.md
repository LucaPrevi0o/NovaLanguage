# Documentation publishing

This project publishes documentation in two different places:

1. Markdown documentation from the repository is mirrored to the GitHub Wiki.
2. Generated Javadoc HTML is deployed to GitHub Pages.

The automation lives in `.github/workflows/docs-publish.yml`.

## Why two targets?

The GitHub Wiki is useful for human-written Markdown pages.

Generated Javadoc is a static HTML site made of many HTML, CSS, JavaScript, and index files. GitHub Pages is the correct target for that output.

## Wiki mirroring

The workflow mirrors these repository files to the Wiki repository:

- `README.md` -> `Home.md`
- `PLAN.md` -> `Compiler-Upgrade-Plan.md`
- `docs/*.md` -> one Wiki page per Markdown file
- generated `_Sidebar.md`
- generated `API-Reference.md`

The Wiki repository is cloned only inside the GitHub Actions runner as `wiki-repo/`.

Do not commit a Wiki clone into the main repository.

If you clone the Wiki locally for debugging, use a temporary ignored folder such as:

```bash
git clone https://github.com/OWNER/REPOSITORY.wiki.git wiki-repo
```

`wiki-repo/`, `.wiki/`, and `wiki/` are ignored by `.gitignore`.

## Wiki prerequisites

The Wiki must exist before the workflow can push to it.

To initialize it:

1. Open the repository on GitHub.
2. Go to the Wiki tab.
3. Create the first page manually.
4. Re-run the `Publish documentation` workflow.

The workflow first checks whether the Wiki repository is reachable. If it is not, it fails with a message explaining what to do.

## Wiki authentication

The workflow first tries to use the default `GITHUB_TOKEN`.

If GitHub refuses writes to the Wiki repository, create a repository secret named:

```text
WIKI_DEPLOY_TOKEN
```

Use a fine-grained token or GitHub App token with enough permission to push to the Wiki repository.

## Javadoc publishing

The workflow generates Javadocs with:

```bash
./mvnw -B -ntp test javadoc:javadoc
```

The Maven Javadoc Plugin writes output to:

```text
target/site/apidocs/
```

The expected Javadoc entrypoint is:

```text
target/site/apidocs/index.html
```

The workflow copies that output to:

```text
_site/javadoc/
```

The expected Pages artifact entrypoints are:

```text
_site/index.html
_site/javadoc/index.html
```

Then it uploads `_site/` as a GitHub Pages artifact and deploys it with GitHub Pages Actions. The workflow fails before deployment if these files are missing, so a broken artifact is not published silently.

## Local Javadoc generation

You do not need to commit generated Javadoc.

To generate it locally:

```bash
./mvnw javadoc:javadoc
```

Then open:

```text
target/site/apidocs/index.html
```

The `target/` directory is already ignored.

## GitHub Pages prerequisite

In the repository settings, configure GitHub Pages to deploy from GitHub Actions.

The workflow uses:

- `actions/configure-pages`
- `actions/upload-pages-artifact`
- `actions/deploy-pages`

The deployed URL is the `page_url` reported by the `Deploy Javadoc to GitHub Pages` job. For a normal project Pages repository, the URL usually has this shape:

```text
https://OWNER.github.io/REPOSITORY/
```

The generated Javadoc is available under:

```text
https://OWNER.github.io/REPOSITORY/javadoc/
```

## Trigger behavior

The workflow runs on:

- pushes to `main` that affect source code, docs, the build file, or the workflow itself;
- manual `workflow_dispatch` runs.

Pull requests do not publish the Wiki or Pages site.
