#!/usr/bin/env python3
"""
The EXPECTED table of ECLIPSECOLLECTIONS.md, for the object-typed public API of Eclipse Collections
(org.eclipse.collections.api, without the generated primitive specialisations and the functional interfaces).

Prints markdown rows `| `pattern` | computed | expected | gap |` that ComposeAnalysisHints.applyExpected reads:
the hints then state what Eclipse Collections MEANS (an ImmutableList is an immutable container with hidden
content, a MutableList a mutable container, a factory stateless), with the computed verdict kept in a comment.

Type rows are name families, as package-qualified prefixes (`org.eclipse.collections.api.list.Immutable*`); the
first matching row wins, so the exceptions come first. The computed cell summarises the source run over the
family's members. Member rows (`type#method`) state receiver modification where the computed verdict contradicts
the method's name on a mutable or read-only type: a mutator (add, put, clear, ...) computed @NotModified, or a pure
query (size, contains, toList, ...) computed @Modified -- the same M1/M2 families as ec-wrong-verdicts.py.

USAGE
    python3 ec-expected-table.py <analysis-results-dir>       # a SOURCE run of EC, --analysis-results-dir
"""
import glob
import json
import re
import sys
from collections import Counter, defaultdict

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

API = 'org.eclipse.collections.api'
IMM = {0: 'mutable', 1: '@FinalFields', 2: '@Immutable(hc = true)', 3: '@Immutable'}
IND = {0: '@Dependent', 1: '@Independent(hc = true)', 2: '@Independent'}

HC_CONTAINER = '@ImmutableContainer(hc = true) @Independent(hc = true)'
STATELESS = '@Immutable(hc = true)'
MUTABLE_CONTAINER = '@Container'
FINAL_FIELDS = '@FinalFields'


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


def in_scope(fqn):
    """the object-typed API: no generated primitive specialisation (the ..primitive packages; the primitive
    iterables at the API root, IntIterable and its lazy twin, stay: a consumer of IntList sees them), no
    functional interface"""
    if not fqn.startswith(API + '.'):
        return False
    pkg, name = fqn.rsplit('.', 1)
    if '.primitive' in pkg or pkg.startswith(API + '.block'):
        return False
    return not fqn.endswith('package-info')


def computed(n):
    d = n.get('data', {})
    level = d.get('immutableType', 0)
    parts = [IMM[level]]
    if d.get('eventuallyImmutableType'):
        parts[0] += ' (eventual)'
    if level < 2:
        ind = d.get('independentType', 0)
        # [level, {}, [[type, method], ...]]: an independence conditional on other types' verdicts (Iterator.remove)
        conditional = isinstance(ind, list)
        parts.append(IND.get(ind[0] if conditional else ind, '@Dependent') + (' (conditional)' if conditional else ''))
        if d.get('containerType') == 1:
            parts.append('@Container')
    return ' '.join(parts)


def summarise(types, matched):
    c = Counter(computed(types[t]) for t in matched)
    if len(c) == 1:
        (v, n), = c.items()
        return f'{v} ({n})'
    return ', '.join(f'{v} ({n})' for v, n in c.most_common())


def family_rows(types):
    """(pattern, expected, gap) in matching order; the computed cell is filled from the members."""
    scope = sorted(t for t in types if in_scope(t) and '.' not in t[len(API) + 1:].split('.')[-1])
    pkgs = sorted({t.rsplit('.', 1)[0] for t in scope})
    rows = []
    # exceptions first: the two non-factory classes of the factory root package
    rows.append((API + '.factory.ServiceLoaderUtils', 'no claim: a utility class', ''))
    rows.append((API + '.factory.ThrowingInvocationHandler', 'no claim', ''))
    rows.append((API + '.factory.*', STATELESS,
                 'F-factory: a factory is stateless; a `Mutable*Factory` creates mutable collections, it is not one'))
    rows.append((API + '.tuple.*', STATELESS, 'a tuple is a value; `Pair.put(Map)` writes its argument, so no container'))
    for pkg in pkgs:
        rel = pkg[len(API):]
        if rel.startswith('.factory') or rel in ('.tuple', '.iterator'):
            continue
        names = [t.rsplit('.', 1)[1] for t in scope if t.rsplit('.', 1)[0] == pkg]
        if any(n.startswith('PartitionImmutable') for n in names):
            rows.append((pkg + '.PartitionImmutable*', HC_CONTAINER,
                         'G1: two immutable collections; computed through the implementor union'))
        if any(n.startswith('Immutable') for n in names):
            rows.append((pkg + '.Immutable*', HC_CONTAINER,
                         'G1: the interface is shared with lazy views and implemented through mutable delegates'))
        if any(n.startswith('Partition') and not n.startswith('PartitionImmutable') for n in names):
            rows.append((pkg + '.Partition*', FINAL_FIELDS,
                         'F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable'))
        if any(n.startswith('Mutable') for n in names):
            rows.append((pkg + '.Mutable*', MUTABLE_CONTAINER, 'a mutable collection stores its arguments, never modifies them'))
        if any(n.startswith('MultiReader') for n in names):
            rows.append((pkg + '.MultiReader*', MUTABLE_CONTAINER, 'a mutable collection behind a lock'))
    return rows, scope


def matches(pattern, fqn):
    return fqn.startswith(pattern[:-1]) if pattern.endswith('*') else pattern == fqn


def main():
    types = load(sys.argv[1])
    rows, scope = family_rows(types)
    print('| pattern | computed | expected | gap |')
    print('|---|---|---|---|')
    claimed = set()
    for pattern, expected, gap in rows:
        matched = [t for t in scope if t not in claimed and matches(pattern, t)]
        claimed.update(matched)
        if not matched:
            continue
        print(f'| `{pattern}` | {summarise(types, matched)} | {expected} | {gap} |')
    # member rows: receiver modification contradicting the name, on the mutable and read-only types
    member_rows = []
    for t in scope:
        s = t.rsplit('.', 1)[1]
        if s.startswith(('Immutable', 'PartitionImmutable')) or 'Iterator' in s or '.factory' in t or '.tuple' in t:
            continue
        for m in members(types[t]):
            if m['name'][0] != 'M':
                continue
            name = m['name'][1:].split('(')[0]
            nonmod = m.get('data', {}).get('nonModifyingMethod') == 1
            if name in MUTATORS and nonmod and s.startswith(('Mutable', 'MultiReader')):
                member_rows.append((t, name, '@NotModified', '@Modified', 'M1: a mutator'))
            elif name in QUERIES and not nonmod:
                member_rows.append((t, name, '@Modified', '@NotModified', 'M2: a pure query'))
    seen = set()
    for t, name, comp, exp, gap in sorted(member_rows):
        if (t, name) in seen:
            continue
        seen.add((t, name))
        print(f'| `{t}#{name}` | {comp} | {exp} | {gap} |')
    print(f'\n<!-- {len(claimed)} of {len(scope)} types in scope carry an expected row; '
          f'{len(seen)} member rows -->', file=sys.stderr)


if __name__ == '__main__':
    main()
