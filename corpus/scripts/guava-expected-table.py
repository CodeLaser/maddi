#!/usr/bin/env python3
"""
The EXPECTED table of GUAVA.md, for the public types of com.google.common.collect (Guava's collections).

Prints markdown rows `| `pattern` | computed | expected | gap |` that ComposeAnalysisHints.applyExpected reads, as
ec-expected-table.py does for Eclipse Collections: the hints state what Guava MEANS (an ImmutableList is an immutable
container with hidden content, its Builder a mutable container, Lists a utility class, a Range a value), with the
computed verdict kept in a comment.

Rows are explicit (Guava's names do not fall into package families as Eclipse Collections' do); the first
matching row wins, so the nested Builders come before their Immutable* owners' prefix rows. Member rows
(`type#method`) state receiver modification where the computed verdict contradicts the method's name on a mutable
or interface type: a mutator (add, put, clear, ...) computed @NotModified, a pure query (size, contains, ...)
computed @Modified.

USAGE
    python3 guava-expected-table.py <analysis-results-dir> [<guava/src/com/google/common/collect>]
        the first is a SOURCE run of guava (--analysis-results-dir); the second, the corpus's source directory of
        the package, restricts the scope to its PUBLIC types (top-level and nested), which is what the jar-composed
        hints carry. Without it every written type of the package is in scope, package-private ones included.
"""
import glob
import json
import os
import re
import sys
from collections import Counter

MUTATORS = {'add', 'addAll', 'put', 'putAll', 'remove', 'removeAll', 'removeIf', 'retainAll', 'clear', 'set',
            'push', 'pop', 'poll', 'offer', 'offerFirst', 'offerLast', 'pollFirst', 'pollLast', 'removeFirst',
            'removeLast', 'addFirst', 'addLast', 'setCount', 'replaceValues', 'forcePut', 'putCoalescing',
            'merge', 'compute', 'computeIfAbsent', 'computeIfPresent', 'replace', 'replaceAll', 'sort'}
QUERIES = {'size', 'isEmpty', 'contains', 'containsAll', 'containsKey', 'containsValue', 'containsEntry',
           'containsRow', 'containsColumn', 'get', 'count', 'toString', 'hashCode', 'equals', 'toArray',
           'indexOf', 'lastIndexOf', 'asList', 'asMap', 'keySet', 'keys', 'values', 'entries', 'entrySet',
           'elementSet', 'rowMap', 'columnMap', 'rowKeySet', 'columnKeySet', 'cellSet', 'row', 'column',
           'peek', 'first', 'last', 'element', 'span', 'encloses', 'enclosesAll', 'intersects', 'subRangeSet',
           'asRanges', 'asDescendingSetOfRanges', 'complement', 'rangeContaining', 'inverse', 'stream',
           'iterator', 'listIterator', 'spliterator', 'reverse', 'subList', 'headSet', 'tailSet', 'subSet',
           'headMap', 'tailMap', 'subMap', 'descendingMap', 'descendingSet', 'descendingIterator',
           'firstKey', 'lastKey', 'comparator', 'lowerBound', 'upperBound', 'hasLowerBound', 'hasUpperBound',
           'lowerBoundType', 'upperBoundType', 'lowerEndpoint', 'upperEndpoint', 'isConnected', 'gap', 'canonical'}

PKG = 'com.google.common.collect'
IMM = {0: 'mutable', 1: '@FinalFields', 2: '@Immutable(hc = true)', 3: '@Immutable'}
IND = {0: '@Dependent', 1: '@Independent(hc = true)', 2: '@Independent'}

HC_CONTAINER = '@ImmutableContainer(hc = true) @Independent(hc = true)'
VALUE = '@Immutable(hc = true)'
MUTABLE_CONTAINER = '@Container'
UTILITY = '@UtilityClass'

# final classes of static methods with a private constructor
UTILITIES = ['Collections2', 'Comparators', 'Interners', 'Iterables', 'Iterators', 'Lists', 'Maps', 'MoreCollectors',
             'Multimaps', 'Multisets', 'ObjectArrays', 'Queues', 'Sets', 'Streams', 'Tables']
# values: no identity of their own, immutable, hidden content in the type parameter
VALUES = ['Range', 'ComparisonChain', 'DiscreteDomain', 'BoundType', 'ImmutableRangeSet', 'ImmutableRangeMap',
          'MapDifference', 'SortedMapDifference']
# the mutable collections, by name
MUTABLE = ['ArrayListMultimap', 'ArrayTable', 'ConcurrentHashMultiset', 'EnumBiMap', 'EnumHashBiMap',
           'EnumMultiset', 'EvictingQueue', 'HashBasedTable', 'HashBiMap', 'HashMultimap', 'HashMultiset',
           'LinkedHashMultimap', 'LinkedHashMultiset', 'LinkedListMultimap', 'MinMaxPriorityQueue',
           'MutableClassToInstanceMap', 'TreeBasedTable', 'TreeMultimap', 'TreeMultiset', 'TreeRangeMap',
           'TreeRangeSet']
# the collection interfaces: a contract with mutators, shared by mutable and immutable implementations
INTERFACES = ['BiMap', 'ClassToInstanceMap', 'ListMultimap', 'Multimap', 'Multiset', 'RangeMap', 'RangeSet',
              'RowSortedTable', 'SetMultimap', 'SortedMultiset', 'SortedSetMultimap', 'Table']


def load(d):
    """Types by dotted FQN; a nested type is an 'S' node (`SBuilder(0)`) among its owner's subs."""
    types = {}

    def walk(n, outer):
        kind = n['name'][0]
        if kind == 'T':
            name = n['name'][1:]
        elif kind == 'S' and outer:
            name = outer + '.' + n['name'][1:].split('(')[0]
        else:
            return
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


def computed(n):
    d = n.get('data', {})
    level = d.get('immutableType', 0)
    parts = [IMM[level]]
    if d.get('eventuallyImmutableType'):
        parts[0] += ' (eventual)'
    if level < 2:
        ind = d.get('independentType', 0)
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


def public_types(source_dir):
    """The public types declared in the package's source directory: top-level, and nested one level down."""
    decl = re.compile(r'^(\s*)((?:(?:public|protected|private|static|final|abstract|sealed|non-sealed)\s+)*)'
                      r'(class|interface|enum|@interface|record)\s+(\w+)')
    result = set()
    for f in sorted(glob.glob(os.path.join(source_dir, '*.java'))):
        top = os.path.basename(f)[:-5]
        top_is_interface = False
        for line in open(f):
            m = decl.match(line)
            if not m:
                continue
            indent, modifiers, kind, name = m.groups()
            if indent == '':
                top_is_interface = kind in ('interface', '@interface')
                if 'public' in modifiers:
                    result.add(PKG + '.' + name)
            elif name != top and ('public' in modifiers or top_is_interface and 'private' not in modifiers):
                # a member type of an interface is implicitly public (Multiset.Entry, Table.Cell)
                result.add(PKG + '.' + top + '.' + name)
    return result


def rows_in_order(types, public=None):
    scope = sorted(t for t in types if t.startswith(PKG + '.') and not t.endswith('package-info')
                   and (public is None or t in public))
    top = [t for t in scope if '.' not in t[len(PKG) + 1:]]
    names = {t.rsplit('.', 1)[1] for t in top}
    rows = []
    # nested builders first: a mutable container that becomes an immutable collection
    for t in scope:
        if t.endswith('.Builder') and t.rsplit('.', 2)[1].startswith('Immutable'):
            rows.append((t, MUTABLE_CONTAINER, 'a builder: mutable, stores its arguments, never modifies them'))
    for n in sorted(names):
        fqn = PKG + '.' + n
        if n in UTILITIES:
            rows.append((fqn, UTILITY, 'static methods only'))
        elif n in VALUES:
            rows.append((fqn, VALUE, 'a value'))
        elif n.startswith('Immutable'):
            rows.append((fqn + '*', HC_CONTAINER,
                         'G1: the abstract class is implemented by package-private classes sharing code with the mutable ones'))
        elif n in MUTABLE:
            rows.append((fqn, MUTABLE_CONTAINER, 'a mutable collection stores its arguments, never modifies them'))
        elif n in INTERFACES:
            rows.append((fqn, MUTABLE_CONTAINER, 'a collection contract with mutators, shared by mutable and immutable implementations'))
    return rows, scope


def matches(pattern, fqn):
    return fqn.startswith(pattern[:-1]) if pattern.endswith('*') else pattern == fqn


def main():
    types = load(sys.argv[1])
    public = public_types(sys.argv[2]) if len(sys.argv) > 2 else None
    rows, scope = rows_in_order(types, public)
    print('| pattern | computed | expected | gap |')
    print('|---|---|---|---|')
    claimed = set()
    for pattern, expected, gap in rows:
        matched = [t for t in scope if t not in claimed and matches(pattern, t)]
        claimed.update(matched)
        if not matched:
            continue
        print(f'| `{pattern}` | {summarise(types, matched)} | {expected} | {gap} |')
    member_rows = []
    for t in scope:
        s = t[len(PKG) + 1:].split('.')[0]
        if s.startswith('Immutable') and not t.endswith('.Builder') or s in UTILITIES or s in VALUES:
            continue
        for m in members(types[t]):
            if m['name'][0] != 'M':
                continue
            name = m['name'][1:].split('(')[0]
            nonmod = m.get('data', {}).get('nonModifyingMethod') == 1
            if name in MUTATORS and nonmod and (s in MUTABLE or s in INTERFACES or t.endswith('.Builder')):
                member_rows.append((t, name, '@NotModified', '@Modified', 'M1: a mutator'))
            elif name in QUERIES and not nonmod and 'Iterator' not in s:
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
