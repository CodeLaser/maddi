#!/usr/bin/env python3
"""
Catalogue the BLATANTLY wrong verdicts of a maddi source run of Eclipse Collections: the engine's work list.

"Blatant" = contradicted by EC's own contract, readable from names alone, with no judgement call:
  T1  an Immutable* type computed below @Immutable(hc)            (pessimistic)
  T2  a mutable type computed @Immutable(hc) or better            (OPTIMISTIC)
  T3  an eventual type verdict in a library with no eventual method (unsound)
  T4  a stateless factory computed below @Immutable(hc)           (pessimistic)
  M1  a mutator (add, put, clear, sortThis, ...) computed @NotModified on a type that can mutate (OPTIMISTIC)
  M2  a pure query (size, isEmpty, contains, get, toList, ...) computed @Modified          (pessimistic)
  M3  any instance method of an Immutable* type computed @Modified                       (pessimistic)
  M4  a read-only argument (addAll's source, equals' other, ...) computed @Modified        (pessimistic)
  M5  a copying method (toList, toArray, newWith, ...) computed @Dependent on a concrete type (pessimistic)
For an ABSTRACT method the verdict is the union over its implementations, so M1/M2 rows also name the
implementations that carry the wrong answer ("blame"): the place to start debugging.

Every method row is also classified by its SOURCE body (--src, the checkout):
  throws     the body only throws (UnsupportedOperationException): a @NotModified mutator is then RIGHT, dropped
  delegates  the body calls the same-named method on a field/getter (this.delegate.size()): a CONSEQUENCE of
             another verdict, usually the abstract method's
  own        anything else: a SEED -- the wrong verdict originates in this body. Debug seeds first.

USAGE
    python3 ec-wrong-verdicts.py <analysis-results-dir> --src <eclipse-collections checkout> [--tsv out.tsv]
Prints a markdown summary (counts, examples, blame, seeds); --tsv writes every row.
"""
import glob
import json
import re
import sys
from collections import Counter, defaultdict

PRIM = r'(Boolean|Byte|Char|Short|Int|Long|Float|Double)'
MUTATORS = {'add', 'addAll', 'addAllIterable', 'put', 'putAll', 'putPair', 'remove', 'removeAll', 'removeAllIterable',
            'removeIf', 'removeIfWith', 'retainAll', 'retainAllIterable', 'clear', 'set', 'sortThis', 'sortThisBy',
            'reverseThis', 'shuffleThis', 'removeIndex', 'removeKey', 'push', 'pop', 'getIfAbsentPut',
            'getIfAbsentPutWith', 'getIfAbsentPutWithKey', 'updateValue', 'updateValueWith', 'addToValue',
            'addOccurrences', 'removeOccurrences', 'setOccurrences', 'forcePut', 'putAllMapIterable', 'addFirst',
            'addLast', 'removeFirst', 'removeLast', 'removeAllKeys', 'removeIfPresent'}
QUERIES = {'size', 'isEmpty', 'notEmpty', 'contains', 'containsAll', 'containsAllIterable', 'containsAllArguments',
           'containsKey', 'containsValue', 'get', 'getFirst', 'getLast', 'getOnly', 'count', 'countWith',
           'anySatisfy', 'allSatisfy', 'noneSatisfy', 'anySatisfyWith', 'allSatisfyWith', 'noneSatisfyWith',
           'detect', 'detectWith', 'detectIfNone', 'toString', 'hashCode', 'equals', 'makeString', 'appendString',
           'toList', 'toSet', 'toBag', 'toArray', 'toSortedList', 'toSortedSet', 'toMap', 'toSortedMap', 'max', 'min',
           'maxBy', 'minBy', 'sum', 'sumOfInt', 'sumOfLong', 'sumOfFloat', 'sumOfDouble', 'average', 'median',
           'injectInto', 'indexOf', 'lastIndexOf', 'occurrencesOf', 'sizeDistinct', 'getIfAbsent', 'getIfAbsentValue',
           'keysView', 'valuesView', 'isFull', 'peek', 'first', 'last', 'getAny'}
COPIES = {'toList', 'toSet', 'toBag', 'toArray', 'toSortedList', 'toSortedSet', 'toSortedArray', 'toMap',
          'toSortedMap', 'newWith', 'newWithout', 'newWithAll', 'newWithoutAll', 'toImmutable', 'toImmutableList',
          'toImmutableSet', 'toImmutableBag', 'toImmutableSortedList', 'toImmutableSortedSet', 'toImmutableMap'}
READ_ONLY_ARGS = {('addAll', None), ('addAllIterable', None), ('containsAll', None), ('containsAllIterable', None),
                  ('equals', None), ('removeAll', None), ('removeAllIterable', None), ('retainAll', None),
                  ('retainAllIterable', None), ('withAll', None), ('withoutAll', None), ('newWithAll', None),
                  ('newWithoutAll', None), ('putAll', None), ('putAllMapIterable', None), ('ofAll', None),
                  ('fromStream', None)}
FUNCTIONAL = re.compile(r'(Function|Predicate|Procedure|Comparator|Supplier|Consumer|Operator|Block)\d*$')


def load(d):
    types = {}

    def walk(n, outer):
        if n['name'][0] != 'T':
            return
        name = n['name'][1:] if not outer else outer + '.' + n['name'][1:]
        types[name] = n
        for s in n.get('subs', []):
            walk(s, name)
        if isinstance(n.get('sub'), dict):
            walk(n['sub'], name)

    for f in glob.glob(d + '/*.json'):
        for n in json.load(open(f)):
            walk(n, '')
    return types


def members(n):
    return n.get('subs', []) + ([n['sub']] if isinstance(n.get('sub'), dict) else [])


def mname(m):
    return m['name'][1:].split('(')[0]


def params(m):
    return [p for p in members(m) if p['name'][0] == 'P']


def ptypes(m):
    inside = m['name'][m['name'].index('(') + 1:-1].split(',')[1:]
    return inside


def imm(n):
    return n.get('data', {}).get('immutableType', 0)


def is_immutable_family(t):
    s = t.split('.')[-1]
    return (s.startswith('Immutable') and 'Factory' not in s and not s.endswith(('Iterator', 'SerializationProxy',
                                                                                   'Builder')))


def is_mutable_family(t):
    s = t.split('.')[-1]
    if '.immutable.' in t or s.startswith(('Immutable', 'Unmodifiable')) or 'Factory' in s:
        return False
    return (s.startswith(('Mutable', 'Synchronized', 'MultiReader', 'FixedSize'))
            or re.search(r'(FastList|UnifiedMap|UnifiedSet|HashBag|HashMap|HashSet|HashBiMap|ArrayStack|TreeSortedMap|'
                         r'TreeSortedSet|TreeBag|ArrayList|Multimap|ListAdapter|SetAdapter|MapAdapter|'
                         r'CollectionAdapter|ArrayAdapter)$', s) is not None) and '.api.' not in t


def is_unmodifying_family(t):
    s = t.split('.')[-1]
    return s.startswith(('Immutable', 'Unmodifiable')) or '.immutable.' in t


class Sources:
    """Method bodies by (type, method name, arity), read from the checkout's main source roots."""

    def __init__(self, root):
        import os
        self.files = {}
        for mod in ('eclipse-collections-api', 'eclipse-collections'):
            for sub in ('src/main/java', 'target/generated-sources/java'):
                base = os.path.join(root, mod, sub)
                for dirpath, _, names in os.walk(base):
                    for nm in names:
                        if nm.endswith('.java'):
                            rel = os.path.relpath(os.path.join(dirpath, nm), base)[:-5].replace(os.sep, '.')
                            self.files[rel] = os.path.join(dirpath, nm)
        self.cache = {}

    def text(self, primary):
        if primary not in self.cache:
            f = self.files.get(primary)
            self.cache[primary] = open(f).read() if f else None
        return self.cache[primary]

    def body(self, t, name, ptypes):
        """ptypes: the method's parameter types as the results spell them (fully qualified, erased)."""
        arity = len(ptypes)
        want = [simple(p) for p in ptypes]
        # the primary type: the longest prefix of t that is a file
        parts = t.split('.')
        for i in range(len(parts), 0, -1):
            primary = '.'.join(parts[:i])
            if primary in self.files:
                break
        else:
            return None
        src = self.text(primary)
        indent = ' ' * (4 * (len(parts) - i + 1))  # EC style: 4 spaces per nesting level
        for m in re.finditer(r'[\s>]' + re.escape(name) + r'\s*\(', src):
            line_start = src.rfind('\n', 0, m.start()) + 1
            line = src[line_start:m.start() + 1]
            if not line.startswith(indent) or line[len(indent)] == ' ':
                continue  # a member of another (nested or enclosing) type
            # parameters up to the matching ')'
            i, depth = m.end(), 1
            while i < len(src) and depth:
                depth += {'(': 1, ')': -1}.get(src[i], 0)
                i += 1
            plist = src[m.end():i - 1].strip()
            flat = re.sub(r'<[^<>]*>', '', re.sub(r'<[^<>]*>', '', re.sub(r'<[^<>]*>', '', plist)))
            flat = re.sub(r'@\w+(\([^)]*\))?\s*', '', flat)
            got = [] if not flat else [simple(x.strip().replace('final ', '').rsplit(' ', 1)[0]) for x in flat.split(',')]
            if len(got) != arity or any(g != w and not (len(w) == 1 or len(g) == 1) for g, w in zip(got, want)):
                continue  # another overload (a one-letter name is a type variable: matches anything)
            rest = src[i:i + 400].lstrip()
            if rest.startswith('throws'):
                rest = rest[rest.index('{'):] if '{' in rest else rest
            if not rest.startswith('{'):
                continue  # a call, or an abstract declaration
            j, depth = src.index('{', i), 0
            k = j
            while k < len(src):
                depth += {'{': 1, '}': -1}.get(src[k], 0)
                k += 1
                if depth == 0:
                    break
            return src[j + 1:k - 1]
        return None

    def kind(self, t, name, ptypes):
        b = self.body(t, name, ptypes)
        if b is None:
            return 'nosource'
        code = re.sub(r'/\*.*?\*/', '', b, flags=re.S)
        code = re.sub(r'//[^\n]*', '', code).strip()
        if re.fullmatch(r'throw new \w+\([^;]*\);', code):
            return 'throws'
        if re.search(r'(this\.)?\w+(\(\))?\.' + re.escape(name) + r'\s*\(', code) and code.count(';') <= 3:
            return 'delegates'
        # a LEAF calls no method: no `x.m(`, no bare `m(` other than `new T(` and the control keywords. Its wrong
        # verdict cannot be inherited from a callee; it originates in this body.
        plain = re.sub(r'"[^"]*"', '""', code)
        calls = [c for c in re.findall(r'(?<![\w.])(new\s+[\w.<>]+|\w+)\s*\(', plain)
                 if not c.startswith('new') and c not in ('if', 'for', 'while', 'switch', 'catch', 'synchronized',
                                                          'return', 'super', 'this')]
        # `new T(this)` hands the receiver to a constructor: that is a call through which a modification can flow
        passes_this = re.search(r'new\s+[\w.<>]+\s*\([^;]*\bthis\b', plain)
        if not calls and not passes_this and not re.search(r'\.\w+\s*\(', plain):
            return 'leaf'
        return 'own'


def simple(type_name):
    """'java.lang.Iterable' -> 'Iterable', 'int...' / 'int[]' -> 'int[]', 'Map.Entry' -> 'Entry'."""
    t = type_name.strip().replace('...', '[]')
    return t.rsplit('.', 1)[-1] if not t.endswith(']') else t.rsplit('.', 1)[-1]


def is_factory(t):
    s = t.split('.')[-1]
    return s.endswith(('FactoryImpl',)) and '.impl.' in t


def public(n):
    # the source run does not record access; nested and generated types are counted, the report says so
    return True


def main():
    d = sys.argv[1]
    src = Sources(sys.argv[sys.argv.index('--src') + 1])
    tsv = sys.argv[sys.argv.index('--tsv') + 1] if '--tsv' in sys.argv else None
    T = load(d)
    rows = []  # (family, element, detail, kind)
    eventual_methods = sum(1 for n in T.values() for m in members(n)
                           if m['name'][0] == 'M' and m.get('data', {}).get('eventualMethod'))

    for t, n in T.items():
        data = n.get('data', {})
        level = imm(n)
        s = t.split('.')[-1]
        if is_immutable_family(t) and level < 2:
            rows.append(('T1', t, None, {0: 'mutable', 1: '@FinalFields'}[level]))
        if is_mutable_family(t) and level >= 2:
            rows.append(('T2', t, None, {2: '@Immutable(hc=true)', 3: '@Immutable'}[level]))
        if data.get('eventuallyImmutableType') and eventual_methods == 0:
            rows.append(('T3', t, None, 'after=' + str(data['eventuallyImmutableType'][0])))
        if is_factory(t) and level < 2 and not s.startswith(('Mutable', 'Synchronized')):
            rows.append(('T4', t, None, {0: 'mutable', 1: '@FinalFields'}[level]))

        for m in members(n):
            if m['name'][0] != 'M':
                continue
            md = m.get('data', {})
            name = mname(m)
            nonmod = md.get('nonModifyingMethod') == 1
            impls = md.get('implementations')
            element = t + '.' + m['name'][1:]
            kind = src.kind(t, name, ptypes(m)) if not impls else 'abstract'
            if name in MUTATORS and nonmod and not is_unmodifying_family(t) and not name.startswith('get'):
                # a mutator on a type that can mutate, computed non-modifying
                if is_mutable_family(t) or '.api.' in t and s.startswith('Mutable'):
                    if kind != 'throws':
                        rows.append(('M1', element, name, kind, blame(T, impls, want_nonmod=False)))
            if name in QUERIES and not nonmod and 'Iterator' not in s and 'MultiReader' not in s:
                rows.append(('M2', element, name, kind, blame(T, impls, want_nonmod=True)))
            elif is_immutable_family(t) and not nonmod and name not in QUERIES:
                rows.append(('M3', element, name, kind, blame(T, impls, want_nonmod=True)))
            if (name, None) in READ_ONLY_ARGS:
                for p, pt in zip(params(m), ptypes(m)):
                    if FUNCTIONAL.search(pt.split('.')[-1]) or pt in ('int', 'long', 'boolean'):
                        continue
                    if p.get('data', {}).get('unmodifiedParameter') != 1:
                        rows.append(('M4', element + ':' + p['name'][1:].split('(')[0], name, kind, pt))
            if name in COPIES and 'independentMethod' not in md and not impls \
                    and not s.startswith(('Unmodifiable', 'Synchronized', 'MultiReader')) and 'Lazy' not in s \
                    and '.lazy.' not in t:
                rows.append(('M5', element, name, kind, None))

    by = defaultdict(list)
    for r in rows:
        by[r[0]].append(r)
    titles = {'T1': 'Immutable* type computed below @Immutable(hc = true)',
              'T2': 'mutable type computed @Immutable(hc = true) or better (OPTIMISTIC)',
              'T3': f'eventual type verdict without any eventual method ({eventual_methods} in the run)',
              'T4': 'stateless factory implementation computed below @Immutable(hc = true)',
              'M1': 'mutator computed @NotModified (OPTIMISTIC)',
              'M2': 'pure query computed @Modified',
              'M3': 'other method of an Immutable* type computed @Modified',
              'M4': 'read-only argument computed @Modified',
              'M5': 'copying method on a concrete type computed @Dependent'}
    for f in sorted(by):
        items = by[f]
        print(f'### {f}: {titles[f]} -- {len(items)}')
        if f[0] == 'T':
            for r in items[:10]:
                print(f'  - {short(r[1])}  {r[3]}')
            print()
            continue
        kinds = Counter(r[3] for r in items)
        print('by body:', ', '.join(f'{k} {v}' for k, v in kinds.most_common()))
        print('by method:', ', '.join(f'{k} {v}' for k, v in Counter(r[2] for r in items).most_common(12)))
        culprits = Counter(c for r in items if r[3] == 'abstract' for c in (r[4] or []))
        if culprits:
            print('abstract rows blame (implementations with the wrong own verdict):',
                  ', '.join(f'{c} ({k})' for c, k in culprits.most_common(8)))
        seeds = [r for r in items if r[3] == 'leaf'] + [r for r in items if r[3] == 'own']
        print(f'SEEDS: {kinds.get("leaf", 0)} leaf (no call at all), then {kinds.get("own", 0)} own; by type:',
              ', '.join(f'{short(k)} {v}' for k, v in Counter(r[1].rsplit('.', 1)[0] if '(' not in r[1].rsplit('.', 1)[0]
                                                             else r[1] for r in seeds).most_common(10)))
        for r in seeds[:10]:
            print(f'  - {short(r[1])}' + (f'  [{short(str(r[4]))}]' if f == 'M4' else ''))
        print()
    if tsv:
        with open(tsv, 'w') as out:
            for r in rows:
                out.write('\t'.join(str(x) if x is not None else '' for x in r) + '\n')


def blame(T, impls, want_nonmod):
    """For an abstract method: the implementing TYPES whose own verdict is the wrong one (modifying where
    want_nonmod, non-modifying otherwise). None for a concrete method."""
    if not impls:
        return None
    out = []
    for impl in impls:
        tname = impl[0][1:]
        path = impl[1:]
        node = T.get(tname)
        if node is None:
            continue
        for step in path:
            node = next((m for m in members(node) if m['name'] == step), None)
            if node is None:
                break
        if node is None:
            continue
        nonmod = node.get('data', {}).get('nonModifyingMethod') == 1
        if nonmod != want_nonmod and not node.get('data', {}).get('implementations'):
            out.append(short(tname))
    return out


def short(s):
    return s.replace('org.eclipse.collections.', '')


if __name__ == '__main__':
    main()
