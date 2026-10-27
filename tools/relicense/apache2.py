#!/usr/bin/env python3
"""
Relicense maddi, maddi-mod and maddi-dist from LGPL-3.0-or-later to Apache-2.0.

Idempotent: a second run changes nothing, so it can be run again after every merge that brings in files written
under the old licence. What it changes:

- the licence paragraph of each file header (Java, Kotlin, TypeScript, C#, Bazel, XML, ...), in whatever comment
  style the header uses; the project line above it is kept as it is;
- every copyright notice of Bart Naudts or CodeLaser BV, in any file, becomes HOLDER's with its range ending in
  YEAR: "Copyright 2020-2025, Bart Naudts, https://..." -> "Copyright 2020-2026, CodeLaser BV, https://...";
  the start year and whatever follows the holder stay;
- COPYING and COPYING.LESSER (GPL/LGPL texts) are removed; a LICENSE holding the LGPL text is replaced, and a
  directory that held a COPYING gets a LICENSE with the Apache text if it has none;
- the licence line of NOTICE, and the header template copyright_file.txt;
- licence metadata: POM licence name and URL, jreleaser license, package.json "license", the Eclipse feature's
  <license> element.

What it leaves alone: prose. README, CONTRIBUTING, the book, build-file comments that discuss the licence need a
person; the script lists every remaining LGPL mention at the end, minus the historical records in KEEP (a released
version stays under the licence it was released with, and the documents saying so stay true).

Usage, from anywhere:

    python3 maddi/tools/relicense/apache2.py                 # maddi and its siblings ../maddi-mod, ../maddi-dist
    python3 maddi/tools/relicense/apache2.py ../maddi-mod    # one repository
    python3 maddi/tools/relicense/apache2.py --dry-run       # list what would change; exit 1 if anything would

It works on the working tree (tracked files plus untracked, non-ignored ones) and does not commit.
"""

import argparse
import os
import re
import subprocess
import sys
from pathlib import Path

SCRIPT = Path(__file__).resolve()
MADDI = SCRIPT.parents[2]
APACHE_TEXT_SOURCE = MADDI / 'maddi-support' / 'LICENSE'

YEAR = 2026
HOLDER = 'CodeLaser BV'
# "Copyright 2020-2025, Bart Naudts" and "Copyright (c) 2022-2026, CodeLaser BV"; jreleaser's copyright.set(...)
COPYRIGHT = re.compile(r'\bCopyright (?P<c>\([cC]\) )?(?P<start>\d{4})(?:-\d{4})?(?P<comma>,?) '
                       r'(?:Bart Naudts|CodeLaser BV)\b')
# the README's "(C) Copyright Bart Naudts, 2020-2026."
COPYRIGHT_HOLDER_FIRST = re.compile(r'\bCopyright (?:Bart Naudts|CodeLaser BV), (?P<start>\d{4})(?:-\d{4})?\b')
JRELEASER_COPYRIGHT = re.compile(r'copyright\.set\("(?P<start>\d{4})(?:-\d{4})? (?:Bart Naudts|CodeLaser BV)"\)')
COPYRIGHT_MENTION = re.compile(r'copyright.*Bart Naudts|Bart Naudts.*copyright', re.I)

APACHE_HEADER = [
    'Licensed under the Apache License, Version 2.0 (the "License");',
    'you may not use this file except in compliance with the License.',
    'You may obtain a copy of the License at',
    '',
    '    http://www.apache.org/licenses/LICENSE-2.0',
    '',
    'Unless required by applicable law or agreed to in writing, software',
    'distributed under the License is distributed on an "AS IS" BASIS,',
    'WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.',
    'See the License for the specific language governing permissions and',
    'limitations under the License.',
]
# for headers that only pointed at the licence ("LGPL v3 or later; see the module sources for the full header.")
APACHE_SHORT = 'Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0'

# files whose leading comment may carry the header
HEADER_SUFFIXES = {'.java', '.kt', '.kts', '.ts', '.js', '.mjs', '.cs', '.bazel', '.bzl', '.bazelrc', '.xml',
                   '.py', '.sh', '.yml', '.yaml', '.properties', '.gradle', '.rs', '.groovy'}
HEADER_NAMES = {'copyright_file.txt', '.bazelrc', 'BUILD', 'WORKSPACE'}
HEADER_SEARCH_LINES = 30

# the first line of a licence paragraph
LICENCE_START = re.compile(r'This program is free software|LGPL v3 or later'
                           r'|Licensed under the GNU Lesser General Public License', re.I)
LGPL_MARK = re.compile(r'Lesser General Public|LGPL', re.I)
COMMENT_END = re.compile(r'^\s*(\*/|-->)')

# historical records, true as they stand: they describe what was released under the LGPL
KEEP = [
    re.compile(r'^docs/project/release-notes-.*\.md$'),
    re.compile(r'^docs/project/landing-surface-checklist\.md$'),
    re.compile(r'^docs/project/opening-up-to-contributors\.md$'),
    re.compile(r'^docs/design/book-vs-support-divergence\.md$'),
    re.compile(r'^tools/rename/'),
    re.compile(r'^tools/relicense/'),
]

METADATA = [
    # Gradle POM block and jreleaser
    (re.compile(r'name\.set\("LGPL-3\.0(?:-or-later|-only)?"\)'), 'name.set("Apache-2.0")'),
    (re.compile(r'url\.set\("https?://www\.gnu\.org/licenses/lgpl-3\.0(?:\.en)?\.html"\)'),
     'url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")'),
    (re.compile(r'license\.set\("LGPL-3\.0(?:-or-later|-only)?"\)'), 'license.set("Apache-2.0")'),
    # package.json
    (re.compile(r'"license"(\s*):(\s*)"LGPL-3\.0(?:-or-later|-only)?"'), r'"license"\1:\2"Apache-2.0"'),
    # Maven <licenses>
    (re.compile(r'<name>\s*(?:GNU Lesser General Public License[^<]*|LGPL-3\.0(?:-or-later|-only)?)\s*</name>'),
     '<name>Apache-2.0</name>'),
    (re.compile(r'<url>\s*https?://www\.gnu\.org/licenses/lgpl-3\.0(?:\.en)?(?:\.html|\.txt)\s*</url>'),
     '<url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>'),
]
FEATURE_LICENSE = re.compile(r'(?P<open><license\s+url=")https?://www\.gnu\.org/licenses/lgpl-3\.0(?:\.en)?\.html'
                             r'(?P<close>">)(?P<body>.*?)(?P<end>\n(?P<indent>[ \t]*)</license>)', re.S)
NOTICE_LINE = re.compile(r'^Licensed under the GNU Lesser General Public Licen[sc]es?,? version 3\.?[ \t]*$',
                         re.I | re.M)


def git_files(repo):
    out = subprocess.run(['git', '-C', str(repo), 'ls-files', '-z', '--cached', '--others', '--exclude-standard'],
                         capture_output=True, check=True).stdout
    return sorted({p for p in out.decode('utf-8').split('\0') if p and (repo / p).is_file()})


def read_text(path):
    try:
        if path.stat().st_size > 4_000_000:
            return None
        with open(path, encoding='utf-8', newline='') as f:
            return f.read()
    except (UnicodeDecodeError, OSError):
        return None


def years(start):
    return start if int(start) >= YEAR else f'{start}-{YEAR}'


def rewrite_copyright(text):
    text = COPYRIGHT.sub(lambda m: f'Copyright {m["c"] or ""}{years(m["start"])}{m["comma"]} {HOLDER}', text)
    text = COPYRIGHT_HOLDER_FIRST.sub(lambda m: f'Copyright {HOLDER}, {years(m["start"])}', text)
    return JRELEASER_COPYRIGHT.sub(lambda m: f'copyright.set("{years(m["start"])} {HOLDER}")', text)


def rewrite_header(text):
    """Replace the LGPL paragraph of the file's leading comment, keeping its comment prefix and line endings."""
    lines = text.splitlines(keepends=True)
    for s, line in enumerate(lines[:HEADER_SEARCH_LINES]):
        m = LICENCE_START.search(line)
        if not m:
            continue
        prefix = line[:m.start()]
        # a prefix is comment furniture only: '', ' * ', '# ', '// ', '  ~ ', '  '
        if prefix.strip() not in ('', '*', '#', '//', '~', '--'):
            return text
        e = s + 1
        while (e < len(lines) and lines[e].startswith(prefix) and lines[e][len(prefix):].strip()
               and not COMMENT_END.match(lines[e])):
            e += 1
        if not LGPL_MARK.search(''.join(lines[s:e])):
            return text
        eol = '\r\n' if line.endswith('\r\n') else '\n'
        blank = prefix.rstrip()
        body = [APACHE_SHORT] if e - s == 1 else APACHE_HEADER
        new = [(prefix + b if b else blank) + eol for b in body]
        return ''.join(lines[:s] + new + lines[e:])
    return text


def rewrite_metadata(text):
    for pattern, repl in METADATA:
        text = pattern.sub(repl, text)

    def feature(m):
        indent = m.group('indent') + '   '
        body = '\n'.join(indent + b if b else '' for b in APACHE_HEADER)
        return (m.group('open') + 'https://www.apache.org/licenses/LICENSE-2.0' + m.group('close') + '\n'
                + body + m.group('end'))

    return FEATURE_LICENSE.sub(feature, text)


def is_gpl_text(text):
    head = text.lstrip()[:200]
    return head.startswith('GNU LESSER GENERAL PUBLIC LICENSE') or head.startswith('GNU GENERAL PUBLIC LICENSE')


def relicense(repo, apache_text, dry_run):
    changes = []

    def write(rel, new):
        changes.append(('M' if (repo / rel).exists() else 'A', rel))
        if not dry_run:
            with open(repo / rel, 'w', encoding='utf-8', newline='') as f:
                f.write(new)

    files = git_files(repo)
    need_license = set()
    for rel in files:
        path = repo / rel
        name = path.name
        text = read_text(path)
        if text is None:
            continue
        if name in ('COPYING', 'COPYING.LESSER') and is_gpl_text(text):
            changes.append(('D', rel))
            if not dry_run:
                path.unlink()
            need_license.add(os.path.dirname(rel))
            continue
        if name in ('LICENSE', 'LICENSE.txt', 'LICENSE.md') and is_gpl_text(text):
            write(rel, apache_text)
            continue
        new = text if any(k.search(rel) for k in KEEP) else rewrite_copyright(text)
        if name == 'NOTICE':
            new = NOTICE_LINE.sub('Licensed under the Apache License, Version 2.0.', new)
        if path.suffix in HEADER_SUFFIXES or name in HEADER_NAMES:
            new = rewrite_header(new)
            new = rewrite_metadata(new)
        elif path.suffix == '.json':
            new = rewrite_metadata(new)
        if new != text:
            write(rel, new)
    for d in sorted(need_license):
        rel = os.path.join(d, 'LICENSE') if d else 'LICENSE'
        if not (repo / rel).exists():
            write(rel, apache_text)
    return changes


def remaining_mentions(repo):
    hits = []
    for rel in git_files(repo):
        if any(k.search(rel) for k in KEEP):
            continue
        text = read_text(repo / rel)
        if text is None:
            continue
        for n, line in enumerate(text.splitlines(), 1):
            if LGPL_MARK.search(line) or COPYRIGHT_MENTION.search(line):
                hits.append(f'{rel}:{n}: {line.strip()[:150]}')
    return hits


def default_repos():
    return [r for r in (MADDI, MADDI.parent / 'maddi-mod', MADDI.parent / 'maddi-dist') if (r / '.git').exists()]


def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n\n')[1], formatter_class=argparse.RawTextHelpFormatter)
    ap.add_argument('repos', nargs='*', type=Path, help='repository roots (default: maddi and its two siblings)')
    ap.add_argument('--dry-run', action='store_true', help='change nothing; exit 1 if a run would change something')
    ap.add_argument('-q', '--quiet', action='store_true', help='summaries only, no per-file lines')
    args = ap.parse_args()

    apache_text = read_text(APACHE_TEXT_SOURCE)
    if not apache_text or not apache_text.lstrip().startswith('Apache License'):
        sys.exit(f'no Apache-2.0 text at {APACHE_TEXT_SOURCE}')

    pending = 0
    for repo in [r.resolve() for r in args.repos] or default_repos():
        if not (repo / '.git').exists():
            sys.exit(f'{repo}: not a git repository root')
        changes = relicense(repo, apache_text, args.dry_run)
        pending += len(changes)
        verb = 'would change' if args.dry_run else 'changed'
        print(f'== {repo.name}: {verb} {len(changes)} file(s)')
        if not args.quiet:
            for kind, rel in changes:
                print(f'   {kind} {rel}')
        hits = [] if args.dry_run else remaining_mentions(repo)
        if hits:
            print(f'   {len(hits)} LGPL or copyright mention(s) left for hand editing:')
            for h in hits:
                print(f'     {h}')
    if args.dry_run and pending:
        sys.exit(1)


if __name__ == '__main__':
    main()
