#!/usr/bin/env python3
"""Create the small GitHub Pages landing page for generated documentation."""

from __future__ import annotations

import os
from pathlib import Path


REPOSITORY = os.environ["GITHUB_REPOSITORY"]
REPOSITORY_OWNER, REPOSITORY_NAME = REPOSITORY.split("/", 1)
ROADMAP_URL = os.environ.get("PROJECT_ROADMAP_URL", f"https://github.com/users/{REPOSITORY_OWNER}/projects/4")
SITE_DIR = Path(".site")
ASSET_DIR = SITE_DIR / "assets"


CSS = """\
:root {
  color-scheme: light;
  --bg: #f6f8fb;
  --surface: #ffffff;
  --text: #182230;
  --muted: #667085;
  --border: #d9e2ec;
  --accent: #2563eb;
  --accent-strong: #1d4ed8;
  --focus: #0f766e;
}

* {
  box-sizing: border-box;
}

body {
  min-height: 100vh;
  margin: 0;
  font-family: ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  color: var(--text);
  background:
    linear-gradient(180deg, rgba(37, 99, 235, 0.08), rgba(37, 99, 235, 0) 280px),
    var(--bg);
}

main {
  width: min(920px, calc(100% - 32px));
  margin: 0 auto;
  padding: 72px 0;
}

.eyebrow {
  margin: 0 0 12px;
  color: var(--focus);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

h1 {
  max-width: 760px;
  margin: 0;
  font-size: clamp(2.2rem, 7vw, 4.75rem);
  line-height: 0.95;
  letter-spacing: 0;
}

.summary {
  max-width: 680px;
  margin: 22px 0 0;
  color: var(--muted);
  font-size: 1.05rem;
  line-height: 1.65;
}

.links {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
  margin: 42px 0 0;
  padding: 0;
  list-style: none;
}

.links a {
  display: block;
  min-height: 128px;
  padding: 22px;
  color: inherit;
  text-decoration: none;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 8px;
  box-shadow: 0 18px 45px rgba(24, 34, 48, 0.08);
  transition: border-color 150ms ease, box-shadow 150ms ease, transform 150ms ease;
}

.links a:hover,
.links a:focus-visible {
  border-color: var(--accent);
  box-shadow: 0 22px 56px rgba(37, 99, 235, 0.16);
  transform: translateY(-2px);
  outline: none;
}

.links strong {
  display: block;
  color: var(--accent-strong);
  font-size: 1rem;
}

.links span {
  display: block;
  margin-top: 10px;
  color: var(--muted);
  line-height: 1.45;
}

footer {
  margin-top: 56px;
  color: var(--muted);
  font-size: 0.9rem;
}

code {
  padding: 0.12em 0.32em;
  background: #eef4ff;
  border: 1px solid #d6e4ff;
  border-radius: 4px;
}
"""


def main() -> None:
    SITE_DIR.mkdir(parents=True, exist_ok=True)
    ASSET_DIR.mkdir(parents=True, exist_ok=True)
    (SITE_DIR / ".nojekyll").touch()
    (ASSET_DIR / "site.css").write_text(CSS, encoding="utf-8")

    index = f"""<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>{REPOSITORY_NAME} Documentation</title>
    <link rel="stylesheet" href="assets/site.css">
  </head>
  <body>
    <main>
      <p class="eyebrow">Generated documentation</p>
      <h1>{REPOSITORY_NAME}</h1>
      <p class="summary">
        This site is generated automatically from the <code>main</code> branch and collects
        the project API reference, human-written Wiki pages, roadmap tracker, and source repository links.
      </p>
      <ul class="links">
        <li>
          <a href="javadoc/">
            <strong>Generated Javadoc</strong>
            <span>Browse the Java API generated from the current source tree.</span>
          </a>
        </li>
        <li>
          <a href="https://github.com/{REPOSITORY}/wiki">
            <strong>Project Wiki</strong>
            <span>Read the mirrored Markdown documentation and design notes.</span>
          </a>
        </li>
        <li>
          <a href="{ROADMAP_URL}">
            <strong>Development Roadmap</strong>
            <span>Open the live GitHub Project used to track current priorities and future work.</span>
          </a>
        </li>
        <li>
          <a href="https://github.com/{REPOSITORY}">
            <strong>Repository</strong>
            <span>Open the source code, workflows, issues, and project history.</span>
          </a>
        </li>
      </ul>
      <footer>Published by GitHub Actions.</footer>
    </main>
  </body>
</html>
"""

    (SITE_DIR / "index.html").write_text(index, encoding="utf-8")


if __name__ == "__main__":
    main()
