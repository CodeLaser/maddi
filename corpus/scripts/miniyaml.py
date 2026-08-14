#!/usr/bin/env python3
"""
A STRICT reader for the small YAML subset the corpus catalogue uses. Standard library only.

WHY NOT PyYAML
    Nothing else in this repo's scripts needs a third-party package (see
    coil-input-configuration.py), and on a PEP 668 machine `pip3 install pyyaml` fails outright —
    a contributor would need brew/apt/a venv before they could read a catalogue entry. Since the
    catalogue is the thing we moved into this repo so that contributors could use it, paying for it
    with an install step defeats the point. catalogue.py uses PyYAML when it happens to be
    importable and this otherwise; `selftest` checks the two agree when both are available.

STRICT, DELIBERATELY
    This understands mappings, sequences, scalars, flow collections, block scalars and comments —
    and RAISES on anything else, naming the file and line. A lenient subset parser is the dangerous
    kind: it turns syntax it does not know into a plausible wrong value. Erroring means an entry
    that is too clever fails loudly the first time it is read, which is when someone can fix it.

NOT SUPPORTED, on purpose: anchors/aliases, multiple documents, tags, complex keys, nested flow
collections, quoted keys.
"""
import re

__all__ = ['load', 'MiniYamlError']


class MiniYamlError(ValueError):
    pass


_KEY = re.compile(r'^(?P<key>[A-Za-z_][\w.-]*)\s*:(?:\s+(?P<val>.*))?$')
_ITEM = re.compile(r'^-(?:\s+(?P<val>.*))?$')
_BLOCK = re.compile(r'^(?P<style>[|>])(?P<chomp>[-+]?)$')


def _strip_comment(line):
    """Remove a trailing #comment that is not inside quotes."""
    out, quote = [], None
    for i, ch in enumerate(line):
        if quote:
            out.append(ch)
            if ch == quote:
                quote = None
        elif ch in '"\'':
            quote = ch
            out.append(ch)
        elif ch == '#' and (i == 0 or line[i - 1] in ' \t'):
            break
        else:
            out.append(ch)
    return ''.join(out).rstrip()


def _scalar(text, where):
    t = text.strip()
    if not t or t in ('~', 'null', 'Null', 'NULL'):
        return None
    if len(t) >= 2 and t[0] == t[-1] and t[0] in '"\'':
        return t[1:-1]
    # Everything below would otherwise fall through to "plain string", which is precisely the
    # silently-wrong outcome this parser exists to avoid. Both were caught by the self-test.
    if t[0] in '"\'':
        raise MiniYamlError(f'{where}: unterminated quote: {t!r}')
    if t[0] in '&*!':
        kind = {'&': 'anchor', '*': 'alias', '!': 'tag'}[t[0]]
        raise MiniYamlError(f'{where}: YAML {kind}s are not supported: {t!r} '
                            '(quote it if you meant a literal string)')
    if t in ('true', 'True', 'TRUE', 'yes', 'on'):
        return True
    if t in ('false', 'False', 'FALSE', 'no', 'off'):
        return False
    if t.startswith('['):
        if not t.endswith(']'):
            raise MiniYamlError(f'{where}: unterminated flow sequence: {t!r}')
        body = t[1:-1].strip()
        return [_scalar(p, where) for p in _split_flow(body, where)] if body else []
    if t.startswith('{'):
        if not t.endswith('}'):
            raise MiniYamlError(f'{where}: unterminated flow mapping: {t!r}')
        body = t[1:-1].strip()
        out = {}
        for part in (_split_flow(body, where) if body else []):
            k, sep, v = part.partition(':')
            if not sep:
                raise MiniYamlError(f'{where}: flow mapping entry without a colon: {part!r}')
            out[k.strip()] = _scalar(v, where)
        return out
    if re.fullmatch(r'-?\d+', t):
        return int(t)
    if re.fullmatch(r'-?\d*\.\d+', t):
        return float(t)
    return t


def _split_flow(body, where):
    """Split on commas that are not inside quotes. Nested flow collections are not supported."""
    parts, cur, quote = [], [], None
    for ch in body:
        if quote:
            cur.append(ch)
            if ch == quote:
                quote = None
        elif ch in '"\'':
            quote = ch
            cur.append(ch)
        elif ch in '[]{}':
            raise MiniYamlError(f'{where}: nested flow collections are not supported')
        elif ch == ',':
            parts.append(''.join(cur))
            cur = []
        else:
            cur.append(ch)
    if quote:
        raise MiniYamlError(f'{where}: unterminated quote')
    if cur:
        parts.append(''.join(cur))
    return [p for p in (p.strip() for p in parts) if p]


class _Reader:
    def __init__(self, text, name):
        self.name = name
        self.lines = []          # (indent, content, lineno) for non-blank, non-comment lines
        self.raw = text.splitlines()
        for n, raw in enumerate(self.raw, 1):
            stripped = _strip_comment(raw)
            if not stripped.strip():
                continue
            self.lines.append((len(raw) - len(raw.lstrip(' ')), stripped.strip(), n))
        self.i = 0

    def where(self, lineno):
        return f'{self.name}:{lineno}'

    def peek(self):
        return self.lines[self.i] if self.i < len(self.lines) else None

    def block_scalar(self, style, chomp, parent_indent, lineno):
        """Gather the indented raw lines following a `|` / `>` marker."""
        body, idx = [], lineno       # raw 1-based line number of the marker
        base = None
        while idx < len(self.raw):
            raw = self.raw[idx]
            if raw.strip():
                ind = len(raw) - len(raw.lstrip(' '))
                if ind <= parent_indent:
                    break
                if base is None:
                    base = ind
                body.append(raw[base:] if len(raw) >= base else raw.strip())
            else:
                body.append('')
            idx += 1
        # advance the token cursor past every token that came from these raw lines
        while self.i < len(self.lines) and self.lines[self.i][2] <= idx:
            self.i += 1
        while body and not body[-1].strip():
            body.pop()
        if style == '|':
            text = '\n'.join(body)
        else:                                    # folded: blank line = paragraph break
            paras, cur = [], []
            for ln in body:
                if ln.strip():
                    cur.append(ln.strip())
                else:
                    paras.append(' '.join(cur))
                    cur = []
            paras.append(' '.join(cur))
            text = '\n'.join(paras)
        if chomp == '-':
            return text
        return text + '\n' if chomp == '+' else text

    def parse(self, indent):
        tok = self.peek()
        if tok is None or tok[0] < indent:
            return None
        if _ITEM.match(tok[1]):
            return self.parse_seq(tok[0])
        return self.parse_map(tok[0])

    def parse_map(self, indent):
        out = {}
        while True:
            tok = self.peek()
            if tok is None or tok[0] < indent:
                return out
            ind, content, lineno = tok
            if ind > indent:
                raise MiniYamlError(f'{self.where(lineno)}: unexpected indent')
            m = _KEY.match(content)
            if not m:
                raise MiniYamlError(f'{self.where(lineno)}: expected `key: value`, got {content!r}')
            self.i += 1
            key, val = m.group('key'), m.group('val')
            if val is None or not val.strip():
                nxt = self.peek()
                out[key] = self.parse(ind + 1) if (nxt and nxt[0] > ind) else None
            else:
                b = _BLOCK.match(val.strip())
                out[key] = (self.block_scalar(b.group('style'), b.group('chomp'), ind, lineno)
                            if b else _scalar(val, self.where(lineno)))

    def parse_seq(self, indent):
        out = []
        while True:
            tok = self.peek()
            if tok is None or tok[0] < indent:
                return out
            ind, content, lineno = tok
            m = _ITEM.match(content)
            if not m:
                return out
            self.i += 1
            val = m.group('val')
            if val is None or not val.strip():
                nxt = self.peek()
                out.append(self.parse(ind + 1) if (nxt and nxt[0] > ind) else None)
            elif _KEY.match(val.strip()):
                # `- key: value`, possibly with more keys indented beneath it
                self.lines.insert(self.i, (ind + 2, val.strip(), lineno))
                out.append(self.parse_map(ind + 2))
            else:
                out.append(_scalar(val, self.where(lineno)))


def load(text, name='<catalogue>'):
    """Parse one YAML document. -> dict | list | scalar | None"""
    r = _Reader(text, name)
    v = r.parse(0)
    if r.peek() is not None:
        ind, content, lineno = r.peek()
        raise MiniYamlError(f'{r.where(lineno)}: trailing content {content!r}')
    return v


# --------------------------------------------------------------------------
# Self-test. `python3 miniyaml.py` — no network, no corpus, under a second.
#
# A hand-rolled parser with no test is a liability, and the failure mode that matters is not a
# crash: it is silently producing a plausible WRONG value. So the cases below pin the shapes the
# catalogue actually uses, and the second half asserts that unsupported syntax RAISES rather than
# being quietly reinterpreted. When PyYAML happens to be importable, every case is also checked
# against it, which is the only real oracle available.
# --------------------------------------------------------------------------

_CASES = [
    ('scalars', 'a: 1\nb: 2.5\nc: true\nd: no\ne: ~\nf: plain text\ng: "quoted: colon"\n',
     {'a': 1, 'b': 2.5, 'c': True, 'd': False, 'e': None, 'f': 'plain text',
      'g': 'quoted: colon'}),
    ('nested', 'top:\n  mid:\n    leaf: v\n  other: 2\n', {'top': {'mid': {'leaf': 'v'}, 'other': 2}}),
    ('sequence', 'xs:\n  - one\n  - two\n', {'xs': ['one', 'two']}),
    ('seq of maps', 'xs:\n  - k: 1\n    j: 2\n  - k: 3\n    j: 4\n',
     {'xs': [{'k': 1, 'j': 2}, {'k': 3, 'j': 4}]}),
    ('flow seq', 'steps: [prep, modification]\n', {'steps': ['prep', 'modification']}),
    ('flow map', 'jdk: { version: 25, vendor: Oracle }\n', {'jdk': {'version': 25, 'vendor': 'Oracle'}}),
    ('empty flow', 'a: []\nb: {}\n', {'a': [], 'b': {}}),
    ('comments', '# lead\na: 1  # trailing\n# between\nb: 2\n', {'a': 1, 'b': 2}),
    ('hash in string', 'a: "not # a comment"\nb: plain#nospace\n',
     {'a': 'not # a comment', 'b': 'plain#nospace'}),
    ('folded', 's: >-\n  one two\n  three\n', {'s': 'one two three'}),
    ('folded paragraphs', 's: >-\n  a b\n\n  c d\n', {'s': 'a b\nc d'}),
    ('literal', 's: |\n  line1\n  line2\n', {'s': 'line1\nline2'}),
    ('literal keeps indent', 's: |\n  a\n    b\n', {'s': 'a\n  b'}),
    ('block then key', 's: >-\n  text\nnext: 1\n', {'s': 'text', 'next': 1}),
    ('empty value', 'build:\nname: x\n', {'build': None, 'name': 'x'}),
    ('null forms', 'a: null\nb: ~\n', {'a': None, 'b': None}),
    ('negative int', 'a: -3\n', {'a': -3}),
    ('key with dash/dot', 'my-key: 1\nmy.key: 2\n', {'my-key': 1, 'my.key': 2}),
]

_MUST_RAISE = [
    ('anchor', 'a: &anchor 1\nb: *anchor\n'),
    ('nested flow', 'a: [[1, 2], 3]\n'),
    ('unterminated flow', 'a: [1, 2\n'),
    ('unterminated quote', "a: 'oops\n"),
    ('bare line', 'a: 1\nnot a mapping line\n'),
]


def selftest():
    failures = []
    try:
        import yaml as oracle
    except ImportError:
        oracle = None

    for label, text, want in _CASES:
        try:
            got = load(text, label)
        except Exception as e:                                   # noqa: BLE001
            failures.append(f'{label}: raised {e!r}')
            continue
        if got != want:
            failures.append(f'{label}: got {got!r}, want {want!r}')
        if oracle is not None:
            ref = oracle.safe_load(text)
            if ref != want:
                failures.append(f'{label}: PyYAML disagrees with the expectation: {ref!r}')

    for label, text in _MUST_RAISE:
        try:
            got = load(text, label)
        except MiniYamlError:
            continue
        except Exception as e:                                   # noqa: BLE001
            failures.append(f'{label}: raised {type(e).__name__}, want MiniYamlError')
            continue
        failures.append(f'{label}: accepted unsupported syntax, returning {got!r} '
                        '— silently wrong is the failure mode this parser must not have')

    for f in failures:
        print('FAIL ' + f)
    total = len(_CASES) + len(_MUST_RAISE)
    print(f'{total - len(failures)}/{total} miniyaml cases pass'
          f"{' (cross-checked against PyYAML)' if oracle else ' (PyYAML absent; no oracle)'}")
    return 1 if failures else 0


if __name__ == '__main__':
    import sys as _sys
    _sys.exit(selftest())
