#!/usr/bin/env python3
"""Create the small GitHub Pages landing page for generated documentation."""

from __future__ import annotations

import os
from pathlib import Path


REPOSITORY = os.environ["GITHUB_REPOSITORY"]
SITE_DIR = Path("_site")


def main() -> None:
    SITE_DIR.mkdir(parents=True, exist_ok=True)
    (SITE_DIR / ".nojekyll").touch()

    index = f"""<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>NovaLanguage Documentation</title>
  </head>
  <body>
    <main>
      <h1>NovaLanguage Documentation</h1>
      <p>This site is generated automatically from the <code>main</code> branch.</p>
      <ul>
        <li><a href="javadoc/">Generated Javadoc</a></li>
        <li><a href="https://github.com/{REPOSITORY}/wiki">Project Wiki</a></li>
        <li><a href="https://github.com/{REPOSITORY}">Repository</a></li>
      </ul>
    </main>
  </body>
</html>
"""

    (SITE_DIR / "index.html").write_text(index, encoding="utf-8")


if __name__ == "__main__":
    main()
