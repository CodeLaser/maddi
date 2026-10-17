// maddi: a modification analyzer for duplication detection and immutability.
// Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
// Licensed under the GNU Lesser General Public License, version 3 or later.
//
// Maddi.JavaCompat: what the Java library does that the BCL does not, or not the same way. maddi's Java -> C#
// translation (maddi-cst-print-csharp) prefers the BCL's own members; it calls these only where Java's behaviour
// differs and the difference can be observed: Map.put returns the previous value, a Deque's removeFirst returns the
// element, String.split takes a regular expression and drops trailing empty strings, DataInputStream reads big-endian.
// Java's byte is signed, so Java's byte[] is sbyte[]: the stream classes here take and give sbyte[], and reinterpret
// them as byte[] for the BCL, which the runtime allows for arrays of one element size.

#nullable enable

using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Reflection;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;

namespace Maddi.JavaCompat;

/// <summary>Java's collection methods whose results the BCL's do not have.</summary>
public static class JavaCollections
{
    /// <summary>Map.put: the previous value, or the default when there was none.</summary>
    public static V? Put<K, V>(this IDictionary<K, V> d, K key, V value)
    {
        d.TryGetValue(key, out var old);
        d[key] = value;
        return old;
    }

    /// <summary>Map.get of a map whose values are a value type: null when the key is absent, as in Java.</summary>
    public static V? GetValueOrNull<K, V>(this IDictionary<K, V> d, K key) where V : struct =>
        d.TryGetValue(key, out var v) ? v : null;

    /// <summary>Map.get on the IDictionary interface: the default when the key is absent.</summary>
    public static V? Get<K, V>(this IDictionary<K, V> d, K key) => d.TryGetValue(key, out var v) ? v : default;

    public static V GetOrDefault<K, V>(this IDictionary<K, V> d, K key, V def) =>
        d.TryGetValue(key, out var v) ? v : def;

    /// <summary>Collection.removeAll: whether anything was removed.</summary>
    public static bool RemoveAllOf<T>(this ICollection<T> c, IEnumerable<T> items)
    {
        var changed = false;
        foreach (var x in items.ToList())
        {
            while (c.Remove(x)) changed = true;
        }
        return changed;
    }

    /// <summary>Collection.removeIf on a set or a map's entries: whether anything was removed.</summary>
    public static bool RemoveIf<T>(this ICollection<T> c, Func<T, bool> p)
    {
        var removed = c.Where(p).ToList();
        foreach (var x in removed) c.Remove(x);
        return removed.Count > 0;
    }

    // a collection of ? is C#'s non-generic interface: its elements are objects
    public static JavaIterator<object> Iterator(this System.Collections.IEnumerable e) =>
        new JavaIterator<object>(e.Cast<object>().ToList());

    public static void ForEach(this System.Collections.IEnumerable e, Action<object> action)
    {
        foreach (var x in e) action(x);
    }

    public static bool Contains(this System.Collections.IEnumerable e, object? item) =>
        e.Cast<object>().Contains(item);

    /// <summary>Iterable.iterator: Java's iterator, with remove.</summary>
    public static JavaIterator<T> Iterator<T>(this IEnumerable<T> e) => new JavaIterator<T>(e);

    /// <summary>Java's listIterator(index): an iterator from the element at index on.</summary>
    public static JavaIterator<T> Iterator<T>(this IList<T> l, int index) => new JavaIterator<T>(l, index);

    public static V? RemoveAndGet<K, V>(this IDictionary<K, V> d, K key)
    {
        if (!d.TryGetValue(key, out var old)) return default;
        d.Remove(key);
        return old;
    }

    // Java's contains(Object), remove(Object), containsKey(Object), get(Object) take any object, which is not there
    // when it is not of the element's or the key's type; C#'s take that type.

    private static bool AsElement<T>(object? o, out T t)
    {
        if (o is T v)
        {
            t = v;
            return true;
        }
        t = default!;
        // a null is an element of a collection of references, as in Java
        return o is null && default(T) is null;
    }

    public static bool ContainsObject<T>(this ICollection<T> c, object? o) => AsElement(o, out T t) && c.Contains(t);

    public static bool RemoveObject<T>(this ICollection<T> c, object? o) => AsElement(o, out T t) && c.Remove(t);

    // a Dictionary has no null key
    public static bool ContainsKeyObject<K, V>(this IDictionary<K, V> d, object? o) => o is K k && d.ContainsKey(k);

    public static V? GetObject<K, V>(this IDictionary<K, V> d, object? o) =>
        o is K k && d.TryGetValue(k, out var v) ? v : default;

    public static V? RemoveKeyObject<K, V>(this IDictionary<K, V> d, object? o) => o is K k ? d.RemoveAndGet(k) : default;

    public static void PutAll<K, V>(this IDictionary<K, V> d, IEnumerable<KeyValuePair<K, V>> other)
    {
        foreach (var (k, v) in other.ToList()) d[k] = v;
    }

    public static V? PutIfAbsent<K, V>(this IDictionary<K, V> d, K key, V value)
    {
        if (d.TryGetValue(key, out var old) && old != null) return old;
        d[key] = value;
        return default;
    }

    public static V ComputeIfAbsent<K, V>(this IDictionary<K, V> d, K key, Func<K, V> f)
    {
        if (d.TryGetValue(key, out var v) && v != null) return v;
        v = f(key);
        if (v != null) d[key] = v;
        return v;
    }

    public static V Compute<K, V>(this IDictionary<K, V> d, K key, Func<K, V?, V> f)
    {
        d.TryGetValue(key, out var old);
        var v = f(key, old);
        if (v == null) d.Remove(key);
        else d[key] = v;
        return v;
    }

    public static V Merge<K, V>(this IDictionary<K, V> d, K key, V value, Func<V, V, V> f)
    {
        var v = d.TryGetValue(key, out var old) && old != null ? f(old, value) : value;
        if (v == null) d.Remove(key);
        else d[key] = v;
        return v;
    }

    /// <summary>Collection.addAll: whether the collection changed.</summary>
    public static bool AddAll<T>(this ICollection<T> c, IEnumerable<T> items)
    {
        var changed = false;
        foreach (var x in items.ToList())
        {
            if (c is ISet<T> set) changed |= set.Add(x);
            else
            {
                c.Add(x);
                changed = true;
            }
        }
        return changed;
    }

    public static bool AddAll<T>(this ICollection<T> c, params T[] items) => c.AddAll((IEnumerable<T>)items);

    public static bool InsertAll<T>(this IList<T> l, int index, IEnumerable<T> items)
    {
        var all = items.ToList();
        for (var i = 0; i < all.Count; i++) l.Insert(index + i, all[i]);
        return all.Count > 0;
    }

    public static bool RetainAll<T>(this ICollection<T> c, IEnumerable<T> keep)
    {
        var kept = new HashSet<T>(keep);
        var removed = c.Where(x => !kept.Contains(x)).ToList();
        foreach (var x in removed) c.Remove(x);
        return removed.Count > 0;
    }

    /// <summary>List.subList, as a copy.</summary>
    public static List<T> SubList<T>(this IList<T> l, int from, int to) => l.Skip(from).Take(to - from).ToList();

    /// <summary>List.sort and Collections.sort: stable, as Java's.</summary>
    public static void Sort<T>(this IList<T> l, Comparison<T>? comparison)
    {
        var sorted = l.Order(Comparer<T>.Create(comparison ?? Comparer<T>.Default.Compare)).ToList();
        for (var i = 0; i < sorted.Count; i++) l[i] = sorted[i];
    }

    public static void Sort<T>(this IList<T> l) => l.Sort(null);

    /// <summary>Collections.rotate: the element at i moves to (i + distance) mod size.</summary>
    public static void Rotate<T>(IList<T> l, int distance)
    {
        var n = l.Count;
        if (n == 0) return;
        var d = ((distance % n) + n) % n;
        if (d == 0) return;
        var copy = l.ToList();
        for (var i = 0; i < n; i++) l[(i + d) % n] = copy[i];
    }

    /// <summary>Collections.shuffle: Fisher-Yates from the end, as Java's.</summary>
    public static void Shuffle<T>(IList<T> l, Random random)
    {
        for (var i = l.Count - 1; i > 0; i--) Swap(l, i, random.Next(i + 1));
    }

    public static void Swap<T>(IList<T> l, int i, int j) => (l[i], l[j]) = (l[j], l[i]);

    /// <summary>Collections.reverse, in place (LINQ's Reverse is a new sequence).</summary>
    public static void ReverseInPlace<T>(IList<T> l)
    {
        for (int i = 0, j = l.Count - 1; i < j; i++, j--) (l[i], l[j]) = (l[j], l[i]);
    }

    /// <summary>List.remove(int): the element removed.</summary>
    public static T RemoveAtAndGet<T>(this IList<T> l, int index)
    {
        var x = l[index];
        l.RemoveAt(index);
        return x;
    }

    /// <summary>List.set: the element replaced.</summary>
    public static T Set<T>(this IList<T> l, int index, T value)
    {
        var old = l[index];
        l[index] = value;
        return old;
    }

    public static T RemoveFirst<T>(this IList<T> l)
    {
        if (l.Count == 0) throw new InvalidOperationException("empty");
        return l.RemoveAtAndGet(0);
    }

    public static T RemoveLast<T>(this IList<T> l)
    {
        if (l.Count == 0) throw new InvalidOperationException("empty");
        return l.RemoveAtAndGet(l.Count - 1);
    }

    public static T? PeekFirst<T>(this IList<T> l) => l.Count == 0 ? default : l[0];

    public static T? PeekLast<T>(this IList<T> l) => l.Count == 0 ? default : l[^1];

    public static T? PollFirst<T>(this IList<T> l) => l.Count == 0 ? default : l.RemoveAtAndGet(0);

    public static T? PollLast<T>(this IList<T> l) => l.Count == 0 ? default : l.RemoveAtAndGet(l.Count - 1);

    public static bool Offer<T>(this IList<T> l, T x)
    {
        l.Add(x);
        return true;
    }

    /// <summary>Iterable.forEach on anything enumerable; a List's own ForEach takes precedence.</summary>
    public static void ForEach<T>(this IEnumerable<T> e, Action<T> action)
    {
        foreach (var x in e) action(x);
    }

    /// <summary>Map.forEach: the action gets the key and the value; of any dictionary, read-only ones too.</summary>
    public static void ForEach<K, V>(this IEnumerable<KeyValuePair<K, V>> d, Action<K, V> action)
    {
        foreach (var e in d) action(e.Key, e.Value);
    }

    /// <summary>List.indexOf of a read-only list, which has no IndexOf of its own.</summary>
    public static int IndexOf<T>(IReadOnlyList<T> list, T x)
    {
        var comparer = EqualityComparer<T>.Default;
        for (int i = 0; i < list.Count; i++)
        {
            if (comparer.Equals(list[i], x)) return i;
        }
        return -1;
    }

    /// <summary>Stream.reduce without an identity: the default (Optional's empty) for an empty sequence.</summary>
    public static T? Reduce<T>(this IEnumerable<T> e, Func<T, T, T> f)
    {
        using var it = e.GetEnumerator();
        if (!it.MoveNext()) return default;
        var acc = it.Current;
        while (it.MoveNext()) acc = f(acc, it.Current);
        return acc;
    }

    public static IEnumerable<T> Sorted<T>(this IEnumerable<T> e, Comparison<T> comparison) =>
        e.Order(Comparer<T>.Create(comparison));

    // Optional<T> is the value itself, or null
    public static void IfPresent<T>(this T? value, Action<T> action)
    {
        if (value != null) action(value);
    }

    public static R? Map<T, R>(this T? value, Func<T, R> f) => value == null ? default : f(value);

    public static T? Filter<T>(this T? value, Func<T, bool> p) => value != null && p(value) ? value : default;

    /// <summary>Optional.stream: the value, or nothing.</summary>
    public static IEnumerable<T> AsStream<T>(this T? value)
    {
        if (value != null) yield return value;
    }
}

/// <summary>
/// Java's NavigableMap and NavigableSet on SortedDictionary and SortedSet. A SortedSet searches by order in log time
/// (GetViewBetween); a SortedDictionary keeps its keys in order but searches by order only in a walk over them. A
/// sub-map is a copy, where Java's is a view.
/// </summary>
public static class JavaNavigable
{
    public static JavaEntry<K, V>? FirstEntry<K, V>(this SortedDictionary<K, V> m) where K : notnull =>
        m.Count == 0 ? null : new JavaEntry<K, V>(m.First());

    public static JavaEntry<K, V>? LastEntry<K, V>(this SortedDictionary<K, V> m) where K : notnull =>
        m.Count == 0 ? null : new JavaEntry<K, V>(m.Last());

    public static JavaEntry<K, V>? PollFirstEntry<K, V>(this SortedDictionary<K, V> m) where K : notnull
    {
        var first = m.FirstEntry();
        if (first != null) m.Remove(first.Key);
        return first;
    }

    public static JavaEntry<K, V>? PollLastEntry<K, V>(this SortedDictionary<K, V> m) where K : notnull
    {
        var last = m.LastEntry();
        if (last != null) m.Remove(last.Key);
        return last;
    }

    /// <summary>The greatest entry below <paramref name="key"/>, or at it when <paramref name="inclusive"/>.</summary>
    private static JavaEntry<K, V>? Below<K, V>(SortedDictionary<K, V> m, K key, bool inclusive) where K : notnull
    {
        JavaEntry<K, V>? found = null;
        foreach (var e in m)
        {
            var c = m.Comparer.Compare(e.Key, key);
            if (c < 0 || inclusive && c == 0) found = new JavaEntry<K, V>(e);
            else break;
        }
        return found;
    }

    /// <summary>The least entry above <paramref name="key"/>, or at it when <paramref name="inclusive"/>.</summary>
    private static JavaEntry<K, V>? Above<K, V>(SortedDictionary<K, V> m, K key, bool inclusive) where K : notnull
    {
        foreach (var e in m)
        {
            var c = m.Comparer.Compare(e.Key, key);
            if (c > 0 || inclusive && c == 0) return new JavaEntry<K, V>(e);
        }
        return null;
    }

    public static JavaEntry<K, V>? FloorEntry<K, V>(this SortedDictionary<K, V> m, K key) where K : notnull => Below(m, key, true);

    public static JavaEntry<K, V>? LowerEntry<K, V>(this SortedDictionary<K, V> m, K key) where K : notnull => Below(m, key, false);

    public static JavaEntry<K, V>? CeilingEntry<K, V>(this SortedDictionary<K, V> m, K key) where K : notnull => Above(m, key, true);

    public static JavaEntry<K, V>? HigherEntry<K, V>(this SortedDictionary<K, V> m, K key) where K : notnull => Above(m, key, false);

    public static K? FloorKey<K, V>(this SortedDictionary<K, V> m, K key) where K : notnull => Below(m, key, true) is { } e ? e.Key : default;

    public static K? LowerKey<K, V>(this SortedDictionary<K, V> m, K key) where K : notnull => Below(m, key, false) is { } e ? e.Key : default;

    public static K? CeilingKey<K, V>(this SortedDictionary<K, V> m, K key) where K : notnull => Above(m, key, true) is { } e ? e.Key : default;

    public static K? HigherKey<K, V>(this SortedDictionary<K, V> m, K key) where K : notnull => Above(m, key, false) is { } e ? e.Key : default;

    public static SortedDictionary<K, V> HeadMap<K, V>(this SortedDictionary<K, V> m, K to, bool inclusive) where K : notnull =>
        Copy(m, e => { var c = m.Comparer.Compare(e.Key, to); return c < 0 || inclusive && c == 0; });

    public static SortedDictionary<K, V> TailMap<K, V>(this SortedDictionary<K, V> m, K from, bool inclusive) where K : notnull =>
        Copy(m, e => { var c = m.Comparer.Compare(e.Key, from); return c > 0 || inclusive && c == 0; });

    public static SortedDictionary<K, V> SubMap<K, V>(this SortedDictionary<K, V> m, K from, bool fromInclusive, K to,
        bool toInclusive) where K : notnull =>
        m.TailMap(from, fromInclusive).HeadMap(to, toInclusive);

    private static SortedDictionary<K, V> Copy<K, V>(SortedDictionary<K, V> m, Func<KeyValuePair<K, V>, bool> keep) where K : notnull
    {
        var copy = new SortedDictionary<K, V>(m.Comparer);
        foreach (var e in m.Where(keep)) copy[e.Key] = e.Value;
        return copy;
    }

    public static T? Floor<T>(this SortedSet<T> s, T x) =>
        s.Count == 0 || s.Comparer.Compare(s.Min, x) > 0 ? default : s.GetViewBetween(s.Min, x).Max;

    public static T? Ceiling<T>(this SortedSet<T> s, T x) =>
        s.Count == 0 || s.Comparer.Compare(s.Max, x) < 0 ? default : s.GetViewBetween(x, s.Max).Min;

    public static T? Lower<T>(this SortedSet<T> s, T x)
    {
        if (s.Count == 0 || s.Comparer.Compare(s.Min, x) >= 0) return default;
        foreach (var e in s.GetViewBetween(s.Min, x).Reverse())
        {
            if (s.Comparer.Compare(e, x) < 0) return e;
        }
        return default;
    }

    public static T? Higher<T>(this SortedSet<T> s, T x)
    {
        if (s.Count == 0 || s.Comparer.Compare(s.Max, x) <= 0) return default;
        foreach (var e in s.GetViewBetween(x, s.Max))
        {
            if (s.Comparer.Compare(e, x) > 0) return e;
        }
        return default;
    }
}

/// <summary>Comparator's factories and combinators, for Comparison delegates.</summary>
public static class JavaComparator
{
    public static Comparison<T> Comparing<T, K>(Func<T, K> key) =>
        (a, b) => Comparer<K>.Default.Compare(key(a), key(b));

    /// <summary>Comparator.comparing with the compared type given, Comparing&lt;T&gt;(o => o.Id): a lambda's
    /// parameter type cannot be inferred from its use.</summary>
    public static Comparison<T> Comparing<T>(Func<T, IComparable> key) =>
        (a, b) => Comparer<IComparable>.Default.Compare(key(a), key(b));

    public static Comparison<T> ThenComparing<T, K>(this Comparison<T> first, Func<T, K> key) =>
        first.ThenComparing(Comparing(key));

    public static Comparison<T> ThenComparing<T>(this Comparison<T> first, Comparison<T> second) =>
        (a, b) =>
        {
            var c = first(a, b);
            return c != 0 ? c : second(a, b);
        };

    public static Comparison<T> Reversed<T>(this Comparison<T> c) => (a, b) => c(b, a);

    public static Comparison<T> ReverseOrder<T>() => (a, b) => Comparer<T>.Default.Compare(b, a);

    public static Comparison<KeyValuePair<K, V>> ComparingByKey<K, V>() =>
        (a, b) => Comparer<K>.Default.Compare(a.Key, b.Key);

    public static Comparison<KeyValuePair<K, V>> ComparingByValue<K, V>() =>
        (a, b) => Comparer<V>.Default.Compare(a.Value, b.Value);
}

/// <summary>
/// What a mapping leaves out of the translation (<c>exclude</c>), where the translated code still reaches it: a call
/// that fails as the Java code would without the excluded library, with the type the call has.
/// </summary>
public static class JavaUnsupported
{
    public static T Value<T>(string what) => throw new NotSupportedException(what + " is not supported in .NET");

    public static void Call(string what) => throw new NotSupportedException(what + " is not supported in .NET");
}

/// <summary>Java's String methods that differ from the BCL's.</summary>
public static class JavaString
{
    /// <summary>String.replaceAll: the replacement in Java's syntax (JavaMatcher.Expand).</summary>
    public static string ReplaceAll(string s, string regex, string replacement) =>
        new JavaMatcher(new Regex(regex), s).ReplaceAll(replacement);

    public static string ReplaceFirst(string s, string regex, string replacement) =>
        new JavaMatcher(new Regex(regex), s).ReplaceFirst(replacement);

    /// <summary>String.split: a regular expression; trailing empty strings removed unless a limit is given.</summary>
    public static string[] SplitRegex(this string s, string regex, int limit = 0)
    {
        if (s.Length == 0) return [""];
        var r = new Regex(regex, RegexOptions.ExplicitCapture);
        var parts = new List<string>();
        var start = 0;
        foreach (Match m in r.Matches(s))
        {
            if (limit > 0 && parts.Count == limit - 1) break;
            if (m.Length == 0 && (m.Index == 0 || m.Index >= s.Length)) continue; // no empty leading substring
            if (m.Index == 0 && m.Length > 0 && start == 0)
            {
                parts.Add("");
                start = m.Length;
                continue;
            }
            parts.Add(s[start..m.Index]);
            start = m.Index + m.Length;
        }
        parts.Add(s[start..]);
        if (limit == 0)
        {
            while (parts.Count > 0 && parts[^1].Length == 0) parts.RemoveAt(parts.Count - 1);
        }
        return parts.ToArray();
    }

    public static sbyte[] GetBytes(string s, Encoding encoding) => encoding.GetBytes(s).AsSigned();

    public static string ValueOf(object? o) => o switch
    {
        null => "null",
        bool b => ValueOf(b),
        double d => ValueOf(d),
        float f => ValueOf(f),
        char[] a => new string(a),
        _ => o.ToString()!
    };

    public static string ValueOf(bool b) => b ? "true" : "false";

    public static string ValueOf(char c) => c.ToString();

    public static string ValueOf(int i) => i.ToString(CultureInfo.InvariantCulture);

    public static string ValueOf(long l) => l.ToString(CultureInfo.InvariantCulture);

    public static string ValueOf(char[] a) => new string(a);

    /// <summary>Double.toString: "1.0", "1.0E10", "NaN", "Infinity".</summary>
    public static string ValueOf(double d)
    {
        if (double.IsNaN(d)) return "NaN";
        if (double.IsInfinity(d)) return d > 0 ? "Infinity" : "-Infinity";
        if (d == 0) return 1 / d < 0 ? "-0.0" : "0.0";
        var abs = Math.Abs(d);
        if (abs >= 1e-3 && abs < 1e7)
        {
            var s = d.ToString("R", CultureInfo.InvariantCulture);
            return s.Contains('.') ? s : s + ".0";
        }
        var e = d.ToString("E16", CultureInfo.InvariantCulture); // d.dddE+xxx
        var mantissa = double.Parse(e[..e.IndexOf('E')], CultureInfo.InvariantCulture)
            .ToString("R", CultureInfo.InvariantCulture);
        if (!mantissa.Contains('.')) mantissa += ".0";
        return mantissa + "E" + int.Parse(e[(e.IndexOf('E') + 1)..], CultureInfo.InvariantCulture);
    }

    public static string ValueOf(float f) => float.IsNaN(f) || float.IsInfinity(f) || f == 0
        ? ValueOf((double)f)
        : ValueOf(double.Parse(f.ToString("R", CultureInfo.InvariantCulture), CultureInfo.InvariantCulture));

    /// <summary>%x: a negative number in its own width's two's complement, as Java's (ffffffff for an int's -1).</summary>
    private static string Hex(object? arg) => arg switch
    {
        int i => ((uint)i).ToString("x", CultureInfo.InvariantCulture),
        long l => ((ulong)l).ToString("x", CultureInfo.InvariantCulture),
        short s => ((ushort)s).ToString("x", CultureInfo.InvariantCulture),
        sbyte b => ((byte)b).ToString("x", CultureInfo.InvariantCulture),
        System.Numerics.BigInteger big => big.Sign < 0 ? "-" + (-big).ToString("x", CultureInfo.InvariantCulture).TrimStart('0')
            : big.IsZero ? "0" : big.ToString("x", CultureInfo.InvariantCulture).TrimStart('0'),
        _ => Convert.ToString(Convert.ToInt64(arg, CultureInfo.InvariantCulture), 16)
    };

    /// <summary>String.format: Java's %-conversions (s, d, x, X, c, b, f, n, %), with flags, width and precision.</summary>
    public static string Format(string format, params object?[] args)
    {
        var sb = new StringBuilder();
        var next = 0;
        var i = 0;
        while (i < format.Length)
        {
            var c = format[i++];
            if (c != '%')
            {
                sb.Append(c);
                continue;
            }
            var spec = Regex.Match(format[i..], @"^(\d+\$)?([-#+ 0,(]*)(\d+)?(\.\d+)?([a-zA-Z%])");
            if (!spec.Success) throw new FormatException("bad format specifier at " + (i - 1) + ": " + format);
            i += spec.Length;
            var conversion = spec.Groups[5].Value[0];
            if (conversion == 'n')
            {
                sb.Append(Environment.NewLine);
                continue;
            }
            if (conversion == '%')
            {
                sb.Append('%');
                continue;
            }
            var arg = spec.Groups[1].Success
                ? args[int.Parse(spec.Groups[1].Value.TrimEnd('$'), CultureInfo.InvariantCulture) - 1]
                : args[next++];
            var flags = spec.Groups[2].Value;
            var precision = spec.Groups[4].Success ? int.Parse(spec.Groups[4].Value[1..], CultureInfo.InvariantCulture) : -1;
            var text = conversion switch
            {
                's' or 'S' => arg is string str ? str : ValueOf(arg),
                'd' => Convert.ToInt64(arg, CultureInfo.InvariantCulture)
                    .ToString(flags.Contains(',') ? "N0" : "D", CultureInfo.InvariantCulture),
                'x' => Hex(arg),
                'X' => Hex(arg).ToUpperInvariant(),
                'c' => arg is char ch ? ch.ToString() : ((char)Convert.ToInt32(arg, CultureInfo.InvariantCulture)).ToString(),
                'b' => ValueOf(arg is bool b ? b : arg != null),
                'f' => Convert.ToDouble(arg, CultureInfo.InvariantCulture)
                    .ToString("F" + (precision < 0 ? 6 : precision), CultureInfo.InvariantCulture),
                _ => throw new FormatException("unsupported conversion %" + conversion)
            };
            if (char.IsUpper(conversion) && conversion != 'X') text = text.ToUpperInvariant();
            if (precision >= 0 && conversion is 's' or 'S' && text.Length > precision) text = text[..precision];
            if (spec.Groups[3].Success)
            {
                var width = int.Parse(spec.Groups[3].Value, CultureInfo.InvariantCulture);
                // %05d of -5 is -0005: the zeros after the sign
                text = flags.Contains('-') ? text.PadRight(width)
                    : flags.Contains('0') && text.StartsWith('-') ? "-" + text[1..].PadLeft(width - 1, '0')
                    : flags.Contains('0') ? text.PadLeft(width, '0') : text.PadLeft(width);
            }
            sb.Append(text);
        }
        return sb.ToString();
    }

    public static StringBuilder Reverse(this StringBuilder sb)
    {
        var chars = sb.ToString().ToCharArray();
        Array.Reverse(chars);
        return sb.Clear().Append(chars);
    }
}

/// <summary>Character's classification as Java has it.</summary>
public static class JavaCharacter
{
    // Character.getType's categories, by Java's numbers
    public const int Unassigned = 0;
    public const int SpaceSeparator = 12;
    public const int LineSeparator = 13;
    public const int ParagraphSeparator = 14;
    public const int Control = 15;
    public const int Format = 16;
    public const int PrivateUse = 18;
    public const int Surrogate = 19;

    private static readonly int[] Types =
    {
        1, 2, 3, 4, 5, // letters: upper, lower, title, modifier, other
        6, 8, 7, // marks: non-spacing, spacing combining, enclosing
        9, 10, 11, // numbers: decimal digit, letter, other
        12, 13, 14, // separators: space, line, paragraph
        15, 16, 19, 18, // control, format, surrogate, private use
        23, 20, 21, 22, 29, 30, 24, // punctuation: connector, dash, open, close, initial quote, final quote, other
        25, 26, 27, 28, // symbols: math, currency, modifier, other
        0, // other, not assigned
    };

    /// <summary>Character.getType: the UnicodeCategory, in Java's numbering.</summary>
    public static int GetType(int c) =>
        Types[(int)CharUnicodeInfo.GetUnicodeCategory(c)];

    public static bool IsJavaIdentifierStart(int c)
    {
        if (c == '$' || c == '_') return true;
        var category = CharUnicodeInfo.GetUnicodeCategory(c);
        // the categories of char.IsLetter, for a code point outside the BMP too
        return category is UnicodeCategory.UppercaseLetter or UnicodeCategory.LowercaseLetter
                   or UnicodeCategory.TitlecaseLetter or UnicodeCategory.ModifierLetter or UnicodeCategory.OtherLetter
                   or UnicodeCategory.LetterNumber
            or UnicodeCategory.ConnectorPunctuation or UnicodeCategory.CurrencySymbol;
    }

    public static bool IsJavaIdentifierPart(int c)
    {
        if (IsJavaIdentifierStart(c)) return true;
        var category = CharUnicodeInfo.GetUnicodeCategory(c);
        return category is UnicodeCategory.DecimalDigitNumber or UnicodeCategory.NonSpacingMark
                   or UnicodeCategory.SpacingCombiningMark
               || IsIdentifierIgnorable(c);
    }

    public static bool IsIdentifierIgnorable(int c) =>
        c is >= 0 and <= 8 or >= 0xE and <= 0x1B or >= 0x7F and <= 0x9F
        || CharUnicodeInfo.GetUnicodeCategory(c) == UnicodeCategory.Format;
}

public static class JavaMath
{
    // Java's: the nearest, ties up; d + 0.5 would round 0.49999999999999994 up to 1. NaN is 0, an infinity the
    // extreme value, as .NET's saturating conversion has it
    public static long Round(double d)
    {
        var floor = Math.Floor(d);
        return (long)(d - floor >= 0.5 ? floor + 1 : floor);
    }

    public static int Round(float f)
    {
        var floor = MathF.Floor(f);
        return (int)(f - floor >= 0.5f ? floor + 1 : floor);
    }

    public static int FloorMod(int x, int y) => ((x % y) + y) % y;

    public static long FloorMod(long x, long y) => ((x % y) + y) % y;

    public static int FloorDiv(int x, int y) => (int)Math.Floor((double)x / y);

    public static long FloorDiv(long x, long y) => x / y - ((x % y != 0 && (x ^ y) < 0) ? 1 : 0);

    /// <summary>BigDecimal.stripTrailingZeros: a decimal divided by 1.000…0 keeps the fewest digits that represent it.</summary>
    public static decimal StripTrailingZeros(decimal d) => d / 1.0000000000000000000000000000m;

    /// <summary>BigDecimal.pow(n): exact, by squaring; a negative exponent throws, as Java's.</summary>
    public static decimal Pow(decimal d, int n)
    {
        if (n < 0) throw new ArithmeticException("Invalid operation");
        decimal result = 1m;
        for (var b = d; n > 0; n >>= 1, b *= b)
        {
            if ((n & 1) == 1) result *= b;
        }
        return result;
    }

    /// <summary>BigDecimal.unscaledValue: the decimal's 96-bit integer, with its sign.</summary>
    public static System.Numerics.BigInteger UnscaledValue(decimal d)
    {
        var bits = decimal.GetBits(d);
        var magnitude = new System.Numerics.BigInteger((uint) bits[0])
                        | new System.Numerics.BigInteger((uint) bits[1]) << 32
                        | new System.Numerics.BigInteger((uint) bits[2]) << 64;
        return bits[3] < 0 ? -magnitude : magnitude;
    }

    /// <summary>Duration.getNano: the nanoseconds within the second, from its whole seconds downwards.</summary>
    public static int Nano(TimeSpan duration)
    {
        var ticks = duration.Ticks % TimeSpan.TicksPerSecond;
        if (ticks < 0) ticks += TimeSpan.TicksPerSecond;
        return (int) (ticks * 100);
    }
}

/// <summary>java.util.function's default methods on C#'s delegates.</summary>
/// <summary>java.time.temporal.ChronoUnit, which is what a TemporalUnit is; ToString is Java's, "Days".</summary>
public enum JavaChronoUnit
{
    Nanos, Micros, Millis, Seconds, Minutes, Hours, HalfDays, Days, Weeks, Months, Years, Decades, Centuries,
    Millennia, Eras, Forever
}

/// <summary>
/// java.time.temporal.Temporal's plus, until and isSupported on the BCL's dates and times, which share no interface
/// for them: DateOnly is a LocalDate, TimeOnly a LocalTime, DateTime a LocalDateTime, DateTimeOffset an Instant or an
/// OffsetDateTime. A unit a value does not support throws, as Java's UnsupportedTemporalTypeException.
/// </summary>
public static class JavaTemporal
{
    private static long Ticks(JavaChronoUnit unit) => unit switch
    {
        JavaChronoUnit.Micros => 10,
        JavaChronoUnit.Millis => TimeSpan.TicksPerMillisecond,
        JavaChronoUnit.Seconds => TimeSpan.TicksPerSecond,
        JavaChronoUnit.Minutes => TimeSpan.TicksPerMinute,
        JavaChronoUnit.Hours => TimeSpan.TicksPerHour,
        JavaChronoUnit.HalfDays => TimeSpan.TicksPerHour * 12,
        _ => throw Unsupported(unit)
    };

    private static int MonthsOf(JavaChronoUnit unit) => unit switch
    {
        JavaChronoUnit.Months => 1,
        JavaChronoUnit.Years => 12,
        JavaChronoUnit.Decades => 120,
        JavaChronoUnit.Centuries => 1200,
        JavaChronoUnit.Millennia => 12000,
        _ => throw Unsupported(unit)
    };

    private static bool TimeBased(JavaChronoUnit unit) => unit <= JavaChronoUnit.HalfDays;

    private static bool DateBased(JavaChronoUnit unit) => unit >= JavaChronoUnit.Days && unit <= JavaChronoUnit.Millennia;

    private static ArgumentOutOfRangeException Unsupported(JavaChronoUnit unit) =>
        new(nameof(unit), "Unsupported unit: " + unit);

    public static bool IsSupported(object? temporal, JavaChronoUnit unit) => temporal switch
    {
        DateOnly => DateBased(unit),
        TimeOnly => TimeBased(unit),
        DateTime or DateTimeOffset => TimeBased(unit) || DateBased(unit),
        _ => false
    };

    // a time-based amount in ticks: a nanosecond is a hundredth of one
    private static long AmountTicks(long amount, JavaChronoUnit unit) =>
        unit == JavaChronoUnit.Nanos ? amount / 100 : checked(amount * Ticks(unit));

    private static DateTime Plus(DateTime t, long amount, JavaChronoUnit unit) => unit switch
    {
        _ when TimeBased(unit) => t.AddTicks(AmountTicks(amount, unit)),
        JavaChronoUnit.Days => t.AddDays(amount),
        JavaChronoUnit.Weeks => t.AddDays(checked(amount * 7)),
        _ when DateBased(unit) => t.AddMonths(checked((int)(amount * MonthsOf(unit)))),
        _ => throw Unsupported(unit)
    };

    public static T Plus<T>(T temporal, long amount, JavaChronoUnit unit)
    {
        object result = temporal switch
        {
            DateOnly d when DateBased(unit) => DateOnly.FromDateTime(Plus(d.ToDateTime(TimeOnly.MinValue), amount, unit)),
            TimeOnly t when TimeBased(unit) => t.Add(TimeSpan.FromTicks(AmountTicks(amount, unit) % TimeSpan.TicksPerDay)),
            DateTime t => Plus(t, amount, unit),
            DateTimeOffset t => new DateTimeOffset(Plus(t.DateTime, amount, unit), t.Offset),
            _ => throw Unsupported(unit)
        };
        return (T)result;
    }

    public static T Minus<T>(T temporal, long amount, JavaChronoUnit unit) =>
        amount == long.MinValue ? Plus(Plus(temporal, long.MaxValue, unit), 1, unit) : Plus(temporal, -amount, unit);

    /// <summary>Temporal.plus(TemporalAmount): a Duration, TimeSpan, or a Period.</summary>
    public static T Plus<T>(T temporal, object amount) => amount switch
    {
        TimeSpan span => Plus(temporal, span.Ticks * 100, JavaChronoUnit.Nanos),
        JavaPeriod p => Plus(Plus(temporal, p.ToTotalMonths(), JavaChronoUnit.Months), p.Days, JavaChronoUnit.Days),
        _ => throw new ArgumentException("Unsupported amount: " + amount)
    };

    public static T Minus<T>(T temporal, object amount) => amount switch
    {
        TimeSpan span => Plus(temporal, -span.Ticks * 100, JavaChronoUnit.Nanos),
        JavaPeriod p => Plus(temporal, p.Negated()),
        _ => throw new ArgumentException("Unsupported amount: " + amount)
    };

    // whole months from one date to another, Java's: a month that is not complete does not count
    private static long Months(DateOnly from, DateOnly to)
    {
        long months = (to.Year - from.Year) * 12L + to.Month - from.Month;
        int days = to.Day - from.Day;
        if (months > 0 && days < 0) months--;
        else if (months < 0 && days > 0) months++;
        return months;
    }

    private static long Until(DateOnly from, DateOnly to, JavaChronoUnit unit) => unit switch
    {
        JavaChronoUnit.Days => to.DayNumber - from.DayNumber,
        JavaChronoUnit.Weeks => (to.DayNumber - from.DayNumber) / 7,
        _ when DateBased(unit) => Months(from, to) / MonthsOf(unit),
        _ => throw Unsupported(unit)
    };

    private static long Until(DateTime from, DateTime to, JavaChronoUnit unit)
    {
        if (TimeBased(unit))
        {
            long ticks = (to - from).Ticks;
            return unit == JavaChronoUnit.Nanos ? checked(ticks * 100) : ticks / Ticks(unit);
        }
        // the end's date, a day nearer when its time of day has not reached the start's, as Java's
        var end = DateOnly.FromDateTime(to);
        var start = DateOnly.FromDateTime(from);
        if (end > start && to.TimeOfDay < from.TimeOfDay) end = end.AddDays(-1);
        else if (end < start && to.TimeOfDay > from.TimeOfDay) end = end.AddDays(1);
        return Until(start, end, unit);
    }

    public static long Until<T>(T from, T to, JavaChronoUnit unit) => (object?)from switch
    {
        DateOnly d when DateBased(unit) => Until(d, (DateOnly)(object)to!, unit),
        TimeOnly t when TimeBased(unit) => Until(DateTime.MinValue + t.ToTimeSpan(),
            DateTime.MinValue + ((TimeOnly)(object)to!).ToTimeSpan(), unit),
        DateTime t => Until(t, (DateTime)(object)to!, unit),
        DateTimeOffset t => Until(t.DateTime, ((DateTimeOffset)(object)to!).ToOffset(t.Offset).DateTime, unit),
        _ => throw Unsupported(unit)
    };
}

/// <summary>java.time.Period: years, months and days, each kept as given; ToString is Java's, P1Y2M3D.</summary>
public readonly record struct JavaPeriod(int Years, int Months, int Days)
{
    public static readonly JavaPeriod Zero = new(0, 0, 0);

    public static JavaPeriod Of(int years, int months, int days) => new(years, months, days);

    public static JavaPeriod OfYears(int years) => new(years, 0, 0);

    public static JavaPeriod OfMonths(int months) => new(0, months, 0);

    public static JavaPeriod OfWeeks(int weeks) => new(0, 0, checked(weeks * 7));

    public static JavaPeriod OfDays(int days) => new(0, 0, days);

    public static JavaPeriod Between(DateOnly start, DateOnly end)
    {
        long totalMonths = (end.Year - start.Year) * 12L + end.Month - start.Month;
        int days = end.Day - start.Day;
        if (totalMonths > 0 && days < 0)
        {
            totalMonths--;
            days = end.DayNumber - start.AddMonths((int)totalMonths).DayNumber;
        }
        else if (totalMonths < 0 && days > 0)
        {
            totalMonths++;
            days -= DateTime.DaysInMonth(end.Year, end.Month);
        }
        return new((int)(totalMonths / 12), (int)(totalMonths % 12), days);
    }

    public int GetYears() => Years;

    public int GetMonths() => Months;

    public int GetDays() => Days;

    public bool IsZero() => this == Zero;

    public bool IsNegative() => Years < 0 || Months < 0 || Days < 0;

    public long ToTotalMonths() => Years * 12L + Months;

    // Java's take a TemporalAmount, a reference: here a Period that may be null, as Java's, which throws
    public JavaPeriod Plus(JavaPeriod? amount)
    {
        var p = amount ?? throw new NullReferenceException();
        return new(checked(Years + p.Years), checked(Months + p.Months), checked(Days + p.Days));
    }

    public JavaPeriod Minus(JavaPeriod? amount)
    {
        var p = amount ?? throw new NullReferenceException();
        return new(checked(Years - p.Years), checked(Months - p.Months), checked(Days - p.Days));
    }

    public JavaPeriod PlusYears(long years) => this with { Years = checked((int)(Years + years)) };

    public JavaPeriod PlusMonths(long months) => this with { Months = checked((int)(Months + months)) };

    public JavaPeriod PlusDays(long days) => this with { Days = checked((int)(Days + days)) };

    public JavaPeriod MultipliedBy(int scalar) =>
        new(checked(Years * scalar), checked(Months * scalar), checked(Days * scalar));

    public JavaPeriod Negated() => MultipliedBy(-1);

    public JavaPeriod Normalized()
    {
        long total = ToTotalMonths();
        return new(checked((int)(total / 12)), (int)(total % 12), Days);
    }

    public override string ToString()
    {
        if (IsZero()) return "P0D";
        var sb = new StringBuilder("P");
        if (Years != 0) sb.Append(Years).Append('Y');
        if (Months != 0) sb.Append(Months).Append('M');
        if (Days != 0) sb.Append(Days).Append('D');
        return sb.ToString();
    }
}

public static class JavaFunctions
{
    public static Action<T> AndThen<T>(this Action<T> first, Action<T> then) => x =>
    {
        first(x);
        then(x);
    };

    public static Func<T, V> AndThen<T, R, V>(this Func<T, R> first, Func<R, V> then) => x => then(first(x));

    public static Func<V, R> Compose<V, T, R>(this Func<T, R> second, Func<V, T> first) => x => second(first(x));

    /// <summary>Stream.peek: each element seen by the action as it passes.</summary>
    public static IEnumerable<T> Peek<T>(this IEnumerable<T> source, Action<T> action)
    {
        foreach (var x in source)
        {
            action(x);
            yield return x;
        }
    }
}

/// <summary>java.lang.Class's names that .NET's Type does not have.</summary>
public static class JavaClass
{
    /// <summary>getCanonicalName: a nested type's FullName has a '+' where Java's has a '.'.</summary>
    public static string? CanonicalName(Type t) => t.FullName?.Replace('+', '.');
}

public static class JavaArrays
{
    /// <summary>Arrays.deepEquals, Objects.deepEquals: arrays element by element, nested ones too.</summary>
    public static bool DeepEquals(object? a, object? b)
    {
        if (ReferenceEquals(a, b)) return true;
        if (a is Array x && b is Array y)
        {
            if (x.Length != y.Length) return false;
            for (var i = 0; i < x.Length; i++)
            {
                if (!DeepEquals(x.GetValue(i), y.GetValue(i))) return false;
            }
            return true;
        }
        return Equals(a, b);
    }

    /// <summary>Arrays.deepHashCode: 31 * h + the element's, nested arrays' deep ones.</summary>
    public static int DeepHashCode(object? a)
    {
        if (a is not Array array) return a?.GetHashCode() ?? 0;
        var h = 1;
        foreach (var e in array) h = unchecked(31 * h + DeepHashCode(e));
        return h;
    }

    public static T[] CopyOf<T>(T[] a, int length)
    {
        var copy = new T[length];
        Array.Copy(a, copy, Math.Min(length, a.Length));
        return copy;
    }

    public static bool Equals<T>(T[]? a, T[]? b) =>
        ReferenceEquals(a, b) || a != null && b != null && a.Length == b.Length
        && a.Zip(b).All(p => object.Equals(p.First, p.Second));

    public static int HashCode<T>(T[]? a)
    {
        if (a == null) return 0;
        var h = 1;
        foreach (var x in a) h = 31 * h + (x?.GetHashCode() ?? 0);
        return h;
    }

    public static string ToString<T>(T[]? a) =>
        a == null ? "null" : "[" + string.Join(", ", a.Select(x => JavaString.ValueOf(x))) + "]";
}

public static class JavaSystem
{
    public static string? GetProperty(string key, string? def = null) => key switch
    {
        "line.separator" => Environment.NewLine,
        "file.separator" => Path.DirectorySeparatorChar.ToString(),
        "path.separator" => Path.PathSeparator.ToString(),
        "user.dir" => Environment.CurrentDirectory,
        "user.home" => Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
        "java.io.tmpdir" => Path.GetTempPath(),
        "os.name" => OsName(),
        _ => def
    };

    /// <summary>Java's os.name: Mac OS X, Linux, Windows 11; .NET's description says Darwin 24.0.0 or macOS 15.</summary>
    private static string OsName() =>
        OperatingSystem.IsMacOS() ? "Mac OS X"
        : OperatingSystem.IsLinux() ? "Linux"
        : OperatingSystem.IsWindows() ? "Windows " + (Environment.OSVersion.Version.Build >= 22000 ? "11"
            : Environment.OSVersion.Version.Major.ToString(CultureInfo.InvariantCulture))
        : OperatingSystem.IsFreeBSD() ? "FreeBSD"
        : System.Runtime.InteropServices.RuntimeInformation.OSDescription;
}

/// <summary>Java's byte[] as the BCL's byte[] and back: the same array, reinterpreted.</summary>
internal static class JavaBytes
{
    internal static byte[] AsUnsigned(this sbyte[] bytes) => (byte[])(object)bytes;

    internal static sbyte[] AsSigned(this byte[] bytes) => (sbyte[])(object)bytes;
}

/// <summary>java.io.ByteArrayInputStream: a MemoryStream over a Java byte[].</summary>
public class ByteArrayInputStream : MemoryStream
{
    public ByteArrayInputStream(sbyte[] bytes) : base(bytes.AsUnsigned(), false)
    {
    }

    public ByteArrayInputStream(sbyte[] bytes, int offset, int length) : base(bytes.AsUnsigned(), offset, length, false)
    {
    }
}

/// <summary>java.io.ByteArrayOutputStream: a MemoryStream that gives a Java byte[].</summary>
public class ByteArrayOutputStream : MemoryStream
{
    public ByteArrayOutputStream()
    {
    }

    public ByteArrayOutputStream(int capacity) : base(capacity)
    {
    }

    public sbyte[] ToByteArray() => ToArray().AsSigned();

    public void Write(int b) => WriteByte((byte)b);

    public void Write(sbyte[] b, int offset, int length) => Write(b.AsUnsigned(), offset, length);

    public int Size() => (int)Length;
}

/// <summary>java.io.DataInputStream: big-endian reads, Java's modified UTF-8.</summary>
public class DataInputStream : IDisposable
{
    private readonly Stream input;

    public DataInputStream(Stream input)
    {
        this.input = input;
    }

    public int Read() => input.ReadByte();

    public int Read(sbyte[] b) => Read(b, 0, b.Length);

    public int Read(sbyte[] b, int offset, int length)
    {
        var n = input.Read(b.AsUnsigned(), offset, length);
        return n == 0 && length > 0 ? -1 : n;
    }

    public void ReadFully(sbyte[] b) => ReadFully(b, 0, b.Length);

    public void ReadFully(sbyte[] b, int offset, int length) => input.ReadExactly(b.AsUnsigned(), offset, length);

    public int SkipBytes(int n)
    {
        var skipped = 0;
        while (skipped < n && input.ReadByte() >= 0) skipped++;
        return skipped;
    }

    public long Skip(long n) => SkipBytes((int)n);

    public int Available() => input.CanSeek ? (int)(input.Length - input.Position) : 0;

    public bool ReadBoolean() => ReadUnsignedByte() != 0;

    public sbyte ReadByte() => (sbyte)ReadUnsignedByte();

    public int ReadUnsignedByte()
    {
        var b = input.ReadByte();
        if (b < 0) throw new EndOfStreamException();
        return b;
    }

    public short ReadShort() => (short)ReadUnsignedShort();

    public int ReadUnsignedShort() => (ReadUnsignedByte() << 8) | ReadUnsignedByte();

    public char ReadChar() => (char)ReadUnsignedShort();

    public int ReadInt() => (ReadUnsignedByte() << 24) | (ReadUnsignedByte() << 16) | (ReadUnsignedByte() << 8)
                            | ReadUnsignedByte();

    public long ReadLong() => ((long)ReadInt() << 32) | (uint)ReadInt();

    public float ReadFloat() => BitConverter.Int32BitsToSingle(ReadInt());

    public double ReadDouble() => BitConverter.Int64BitsToDouble(ReadLong());

    public string ReadUTF()
    {
        var length = ReadUnsignedShort();
        var bytes = new byte[length];
        input.ReadExactly(bytes, 0, length);
        var sb = new StringBuilder(length);
        for (var i = 0; i < length;)
        {
            int a = bytes[i++];
            if (a < 0x80) sb.Append((char)a);
            else if ((a & 0xE0) == 0xC0) sb.Append((char)(((a & 0x1F) << 6) | (bytes[i++] & 0x3F)));
            else sb.Append((char)(((a & 0x0F) << 12) | ((bytes[i++] & 0x3F) << 6) | (bytes[i++] & 0x3F)));
        }
        return sb.ToString();
    }

    public virtual void Close() => input.Dispose();

    public void Dispose()
    {
        Close();
        GC.SuppressFinalize(this);
    }
}

/// <summary>java.util.BitSet: a growable set of bits.</summary>
public class BitSet : ICloneable
{
    private ulong[] words;

    public BitSet() : this(64)
    {
    }

    public BitSet(int bits)
    {
        words = new ulong[Math.Max(1, (bits + 63) >> 6)];
    }

    private void Ensure(int word)
    {
        if (word >= words.Length) Array.Resize(ref words, Math.Max(words.Length * 2, word + 1));
    }

    public bool Get(int i) => (i >> 6) < words.Length && (words[i >> 6] & (1UL << (i & 63))) != 0;

    public void Set(int i)
    {
        Ensure(i >> 6);
        words[i >> 6] |= 1UL << (i & 63);
    }

    public void Set(int i, bool value)
    {
        if (value) Set(i);
        else Clear(i);
    }

    public void Set(int from, int to)
    {
        for (var i = from; i < to; i++) Set(i);
    }

    public void Clear(int i)
    {
        if ((i >> 6) < words.Length) words[i >> 6] &= ~(1UL << (i & 63));
    }

    public void Clear() => Array.Clear(words);

    public int NextSetBit(int from)
    {
        for (var i = Math.Max(0, from); i < words.Length * 64; i++)
        {
            if (Get(i)) return i;
        }
        return -1;
    }

    public int NextClearBit(int from)
    {
        var i = Math.Max(0, from);
        while (Get(i)) i++;
        return i;
    }

    /// <summary>The index of the highest set bit, plus one.</summary>
    public int Length()
    {
        for (var w = words.Length - 1; w >= 0; w--)
        {
            if (words[w] != 0) return w * 64 + 64 - System.Numerics.BitOperations.LeadingZeroCount(words[w]);
        }
        return 0;
    }

    public int Size() => words.Length * 64;

    public bool IsEmpty() => words.All(w => w == 0);

    public int Cardinality() => words.Sum(w => System.Numerics.BitOperations.PopCount(w));

    public void Or(BitSet other)
    {
        Ensure(other.words.Length - 1);
        for (var i = 0; i < other.words.Length; i++) words[i] |= other.words[i];
    }

    public void And(BitSet other)
    {
        for (var i = 0; i < words.Length; i++) words[i] &= i < other.words.Length ? other.words[i] : 0;
    }

    public void AndNot(BitSet other)
    {
        for (var i = 0; i < Math.Min(words.Length, other.words.Length); i++) words[i] &= ~other.words[i];
    }

    public void Xor(BitSet other)
    {
        Ensure(other.words.Length - 1);
        for (var i = 0; i < other.words.Length; i++) words[i] ^= other.words[i];
    }

    public bool Intersects(BitSet other)
    {
        for (var i = 0; i < Math.Min(words.Length, other.words.Length); i++)
        {
            if ((words[i] & other.words[i]) != 0) return true;
        }
        return false;
    }

    public object Clone()
    {
        var copy = (BitSet)MemberwiseClone();
        copy.words = (ulong[])words.Clone();
        return copy;
    }

    public override bool Equals(object? obj)
    {
        if (obj is not BitSet other) return false;
        var n = Math.Max(words.Length, other.words.Length);
        for (var i = 0; i < n; i++)
        {
            if ((i < words.Length ? words[i] : 0) != (i < other.words.Length ? other.words[i] : 0)) return false;
        }
        return true;
    }

    public override int GetHashCode()
    {
        ulong h = 1234;
        for (var i = words.Length - 1; i >= 0; i--) h ^= words[i] * (ulong)(i + 1);
        return (int)((h >> 32) ^ h);
    }

    public override string ToString()
    {
        var sb = new StringBuilder("{");
        for (var i = NextSetBit(0); i >= 0; i = NextSetBit(i + 1))
        {
            if (sb.Length > 1) sb.Append(", ");
            sb.Append(i);
        }
        return sb.Append('}').ToString();
    }
}

/// <summary>
/// Java's Iterator over a C# enumerable. A list is walked by index, so that Remove removes the element just returned;
/// any other enumerable is walked over a snapshot, and Remove removes the element from it when it is a collection.
/// </summary>
public interface IJavaIterator<T>
{
    bool HasNext();

    T Next();

    void Remove() => throw new NotSupportedException("remove");

    void ForEachRemaining(Action<T> action)
    {
        while (HasNext()) action(Next());
    }
}

public static class JavaIterators
{
    /// <summary>A Java iterator as C#'s enumerator: a class with iterator() is an IEnumerable.</summary>
    public static IEnumerator<T> AsEnumerator<T>(this IJavaIterator<T> iterator)
    {
        while (iterator.HasNext()) yield return iterator.Next();
    }
}

public class JavaIterator<T> : IJavaIterator<T>
{
    private readonly IList<T>? list;
    private readonly ICollection<T>? collection;
    private readonly IEnumerator<T>? enumerator;
    private int index;
    private bool hasPeeked;
    private bool peeked;
    private T? last;

    public JavaIterator(IEnumerable<T> source)
    {
        list = source as IList<T>;
        if (list == null)
        {
            collection = source as ICollection<T>;
            enumerator = (collection != null ? source.ToList() : source).GetEnumerator();
        }
    }

    /// <summary>Over a list, from the element at start on.</summary>
    public JavaIterator(IList<T> source, int start) : this(source)
    {
        if (start < 0 || start > source.Count) throw new ArgumentOutOfRangeException(nameof(start));
        index = start;
    }

    public bool HasNext()
    {
        if (list != null) return index < list.Count;
        if (!hasPeeked)
        {
            peeked = enumerator!.MoveNext();
            hasPeeked = true;
        }
        return peeked;
    }

    public T Next()
    {
        if (!HasNext()) throw new InvalidOperationException("no next element");
        if (list != null) return last = list[index++];
        hasPeeked = false;
        return last = enumerator!.Current;
    }

    public void Remove()
    {
        if (list != null) list.RemoveAt(--index);
        else if (collection != null) collection.Remove(last!);
        else throw new NotSupportedException("remove");
    }

    public void ForEachRemaining(Action<T> action)
    {
        while (HasNext()) action(Next());
    }
}

/// <summary>java.io.File: a path, with the queries Java asks of it.</summary>
public class JavaFile
{
    private readonly string path;

    public JavaFile(string path) => this.path = path;

    public JavaFile(string? parent, string child) => path = parent == null ? child : Path.Combine(parent, child);

    public JavaFile(JavaFile? parent, string child) : this(parent?.path, child)
    {
    }

    public string GetPath() => path;

    public string GetName() => Path.GetFileName(path);

    public string GetAbsolutePath() => Path.GetFullPath(path);

    public JavaFile GetAbsoluteFile() => new JavaFile(GetAbsolutePath());

    public string GetCanonicalPath() => Path.GetFullPath(path);

    public JavaFile GetCanonicalFile() => new JavaFile(GetCanonicalPath());

    // null for a bare name, as Java's; GetDirectoryName gives ""
    public string? GetParent() => Path.GetDirectoryName(path) is { Length: > 0 } parent ? parent : null;

    public JavaFile? GetParentFile() => GetParent() is { } p ? new JavaFile(p) : null;

    public bool Exists() => File.Exists(path) || Directory.Exists(path);

    public bool IsDirectory() => Directory.Exists(path);

    public bool IsFile() => File.Exists(path);

    public long Length() => File.Exists(path) ? new FileInfo(path).Length : 0;

    public long LastModified() =>
        Exists() ? new DateTimeOffset(File.GetLastWriteTimeUtc(path)).ToUnixTimeMilliseconds() : 0;

    public bool Mkdirs()
    {
        if (Directory.Exists(path)) return false;
        Directory.CreateDirectory(path);
        return true;
    }

    public bool Mkdir() => Mkdirs();

    public bool Delete()
    {
        if (File.Exists(path)) File.Delete(path);
        else if (Directory.Exists(path)) Directory.Delete(path);
        else return false;
        return true;
    }

    public bool CreateNewFile()
    {
        if (Exists()) return false;
        File.Create(path).Dispose();
        return true;
    }

    public string[]? List() => Directory.Exists(path)
        ? Directory.EnumerateFileSystemEntries(path).Select(p => Path.GetFileName(p)).ToArray()
        : null;

    public JavaFile[]? ListFiles() => Directory.Exists(path)
        ? Directory.EnumerateFileSystemEntries(path).Select(p => new JavaFile(p)).ToArray()
        : null;

    public JavaFile[]? ListFiles(Func<JavaFile, bool> filter) => ListFiles()?.Where(filter).ToArray();

    public bool RenameTo(JavaFile dest)
    {
        if (File.Exists(path)) File.Move(path, dest.path);
        else if (Directory.Exists(path)) Directory.Move(path, dest.path);
        else return false;
        return true;
    }

    public bool IsAbsolute() => Path.IsPathRooted(path);

    public override string ToString() => path;

    public override bool Equals(object? o) => o is JavaFile f && f.path == path;

    public override int GetHashCode() => path.GetHashCode();
}

/// <summary>InputStream's methods the BCL's Stream does not have.</summary>
public static class JavaStreams
{
    public static sbyte[] ReadAllBytes(Stream s)
    {
        using var memory = new MemoryStream();
        s.CopyTo(memory);
        return memory.ToArray().AsSigned();
    }

    public static long Skip(Stream s, long n)
    {
        long skipped = 0;
        while (skipped < n && s.ReadByte() >= 0) skipped++;
        return skipped;
    }
}

/// <summary>java.util.regex.Matcher: a Regex applied to an input, step by step.</summary>
public class JavaMatcher
{
    private readonly Regex regex;
    private readonly string input;
    private Match? match;

    private Match Current => match ?? throw new InvalidOperationException("no match");

    public JavaMatcher(Regex regex, string input)
    {
        this.regex = regex;
        this.input = input;
    }

    public bool Find()
    {
        match = match == null ? regex.Match(input) : match.NextMatch();
        return match.Success;
    }

    public bool Matches()
    {
        // \z: .NET's $, as Java's, also matches before a final newline, which matches() does not
        match = Regex.Match(input, "^(?:" + regex + ")\\z", regex.Options);
        return match.Success;
    }

    public bool LookingAt()
    {
        match = Regex.Match(input, "^(?:" + regex + ")", regex.Options);
        return match.Success;
    }

    public string Group() => Current.Value;

    public string? Group(int group) => Current.Groups[group].Success ? Current.Groups[group].Value : null;

    public string? Group(string name) => Current.Groups[name].Success ? Current.Groups[name].Value : null;

    public int GroupCount() => regex.GetGroupNumbers().Length - 1;

    public int Start() => Current.Index;

    public int End() => Current.Index + Current.Length;

    public string ReplaceAll(string replacement) => regex.Replace(input, m => Expand(m, replacement));

    public string ReplaceFirst(string replacement) => regex.Replace(input, m => Expand(m, replacement), 1);

    private int appendPosition;

    /// <summary>Appends the input up to the match and the replacement, in Java's syntax: $n, ${name}, \c.</summary>
    public JavaMatcher AppendReplacement(StringBuilder sb, string replacement)
    {
        sb.Append(input, appendPosition, Current.Index - appendPosition).Append(Expand(Current, replacement));
        appendPosition = Current.Index + Current.Length;
        return this;
    }

    /// <summary>A replacement in Java's syntax for a match: $n and ${name} are groups, \c is c.</summary>
    internal static string Expand(Match match, string replacement)
    {
        var sb = new StringBuilder();
        for (int i = 0; i < replacement.Length; i++)
        {
            char c = replacement[i];
            if (c == '\\' && i + 1 < replacement.Length)
            {
                sb.Append(replacement[++i]);
            }
            else if (c == '$' && i + 1 < replacement.Length && replacement[i + 1] == '{')
            {
                int close = replacement.IndexOf('}', i);
                sb.Append(match.Groups[replacement.Substring(i + 2, close - i - 2)].Value);
                i = close;
            }
            else if (c == '$' && i + 1 < replacement.Length && char.IsDigit(replacement[i + 1]))
            {
                sb.Append(match.Groups[replacement[++i] - '0'].Value);
            }
            else
            {
                sb.Append(c);
            }
        }
        return sb.ToString();
    }

    public StringBuilder AppendTail(StringBuilder sb) => sb.Append(input, appendPosition, input.Length - appendPosition);

    /// <summary>A literal replacement: \ and $ escaped.</summary>
    public static string QuoteReplacement(string s) => s.Replace("\\", "\\\\").Replace("$", "\\$");
}

/// <summary>UUID's name-based factory: a version 3 (MD5) UUID, as Java computes it.</summary>
public static class JavaUuid
{
    public static Guid NameUuidFromBytes(sbyte[] name)
    {
        byte[] hash = MD5.HashData(name.AsUnsigned());
        hash[6] = (byte) ((hash[6] & 0x0f) | 0x30);
        hash[8] = (byte) ((hash[8] & 0x3f) | 0x80);
        return new Guid(hash, bigEndian: true);
    }
}

/// <summary>MessageDigest.getInstance: Java's algorithm names.</summary>
public static class JavaDigest
{
    public static HashAlgorithm GetInstance(string algorithm) => algorithm.ToUpperInvariant() switch
    {
        "SHA-256" => SHA256.Create(),
        "SHA-384" => SHA384.Create(),
        "SHA-512" => SHA512.Create(),
        "SHA-1" or "SHA" => SHA1.Create(),
        "MD5" => MD5.Create(),
        _ => throw new CryptographicException(algorithm + " MessageDigest not available")
    };
}

/// <summary>java.util.Enumeration over a C# enumerable.</summary>
public class JavaEnumeration<T>
{
    private readonly IEnumerator<T> enumerator;
    private bool hasPeeked;
    private bool peeked;

    public JavaEnumeration(IEnumerable<T> source) => enumerator = source.GetEnumerator();

    public bool HasMoreElements()
    {
        if (!hasPeeked)
        {
            peeked = enumerator.MoveNext();
            hasPeeked = true;
        }
        return peeked;
    }

    public T NextElement()
    {
        if (!HasMoreElements()) throw new InvalidOperationException("no more elements");
        hasPeeked = false;
        return enumerator.Current;
    }
}

/// <summary>java.util.zip.ZipEntry: an entry of an archive being read, or one to write.</summary>
public class JavaZipEntry
{
    internal readonly ZipArchiveEntry? Entry;
    private readonly string name;
    internal DateTimeOffset? Time;

    public JavaZipEntry(string name) => this.name = name;

    internal JavaZipEntry(ZipArchiveEntry entry)
    {
        Entry = entry;
        name = entry.FullName;
    }

    public string GetName() => name;

    public bool IsDirectory() => name.EndsWith("/");

    public long GetSize() => Entry?.Length ?? -1;

    public long GetTime() => (Time ?? Entry?.LastWriteTime)?.ToUnixTimeMilliseconds() ?? -1;

    public void SetTime(long time) => Time = DateTimeOffset.FromUnixTimeMilliseconds(time);

    public override string ToString() => name;
}

/// <summary>java.util.zip.ZipFile: an archive opened for reading.</summary>
public class JavaZipFile : IDisposable
{
    protected readonly ZipArchive Archive;

    public JavaZipFile(string path) => Archive = ZipFile.OpenRead(path);

    public JavaZipFile(JavaFile file) : this(file.GetPath())
    {
    }

    public JavaEnumeration<JavaZipEntry> Entries() =>
        new JavaEnumeration<JavaZipEntry>(Archive.Entries.Select(e => new JavaZipEntry(e)).ToList());

    public JavaZipEntry? GetEntry(string name) => Archive.GetEntry(name) is { } e ? new JavaZipEntry(e) : null;

    public Stream? GetInputStream(JavaZipEntry entry) =>
        (entry.Entry ?? Archive.GetEntry(entry.GetName()))?.Open();

    public int Size() => Archive.Entries.Count;

    public void Close() => Archive.Dispose();

    public void Dispose() => Archive.Dispose();
}

/// <summary>java.util.jar.JarFile: a zip archive with a manifest.</summary>
public class JavaJarFile : JavaZipFile
{
    public const string ManifestName = "META-INF/MANIFEST.MF";

    public JavaJarFile(string path) : base(path)
    {
    }

    public JavaJarFile(JavaFile file) : base(file)
    {
    }

    public JavaManifest? GetManifest()
    {
        var entry = Archive.GetEntry(ManifestName);
        if (entry == null) return null;
        using var stream = entry.Open();
        return new JavaManifest(stream);
    }
}

/// <summary>java.util.jar.Manifest: its bytes, read and written as they are.</summary>
public class JavaManifest
{
    private readonly byte[] bytes;

    public JavaManifest() => bytes = Encoding.UTF8.GetBytes("Manifest-Version: 1.0\r\n\r\n");

    public JavaManifest(Stream input)
    {
        using var memory = new MemoryStream();
        input.CopyTo(memory);
        bytes = memory.ToArray();
    }

    public void Write(Stream output) => output.Write(bytes, 0, bytes.Length);
}

/// <summary>java.util.zip.ZipOutputStream: an archive written entry by entry.</summary>
public class JavaZipOutputStream : Stream
{
    private readonly ZipArchive archive;
    private Stream? current;

    public JavaZipOutputStream(Stream output) => archive = new ZipArchive(output, ZipArchiveMode.Create);

    public void PutNextEntry(JavaZipEntry entry)
    {
        CloseEntry();
        var created = archive.CreateEntry(entry.GetName());
        if (entry.Time is { } time) created.LastWriteTime = time;
        current = created.Open();
    }

    public void CloseEntry()
    {
        current?.Dispose();
        current = null;
    }

    public void Write(sbyte[] buffer, int offset, int count) => Write(buffer.AsUnsigned(), offset, count);

    public override void Write(byte[] buffer, int offset, int count) => Current.Write(buffer, offset, count);

    public override void WriteByte(byte value) => Current.WriteByte(value);

    private Stream Current => current ?? throw new InvalidOperationException("no current entry");

    public override void Flush() => current?.Flush();

    public void Close() => Dispose();

    protected override void Dispose(bool disposing)
    {
        if (disposing)
        {
            CloseEntry();
            archive.Dispose();
        }
        base.Dispose(disposing);
    }

    public override bool CanRead => false;
    public override bool CanSeek => false;
    public override bool CanWrite => true;
    public override long Length => throw new NotSupportedException();

    public override long Position
    {
        get => throw new NotSupportedException();
        set => throw new NotSupportedException();
    }

    public override int Read(byte[] buffer, int offset, int count) => throw new NotSupportedException();
    public override long Seek(long offset, SeekOrigin origin) => throw new NotSupportedException();
    public override void SetLength(long value) => throw new NotSupportedException();
}

/// <summary>AbstractMap.SimpleEntry: a key and a value, as a class that the program's classes may extend.</summary>
public class JavaEntry<K, V>
{
    public K Key { get; }
    public V Value { get; private set; }

    public JavaEntry(K key, V value)
    {
        Key = key;
        Value = value;
    }

    public JavaEntry(KeyValuePair<K, V> entry) : this(entry.Key, entry.Value)
    {
    }

    public K GetKey() => Key;

    public V GetValue() => Value;

    public V SetValue(V value)
    {
        var old = Value;
        Value = value;
        return old;
    }

    public static implicit operator KeyValuePair<K, V>(JavaEntry<K, V> e) => new KeyValuePair<K, V>(e.Key, e.Value);

    public override bool Equals(object? o) =>
        o is JavaEntry<K, V> e && Equals(Key, e.Key) && Equals(Value, e.Value);

    public override int GetHashCode() => (Key?.GetHashCode() ?? 0) ^ (Value?.GetHashCode() ?? 0);

    public override string ToString() => Key + "=" + Value;
}

/// <summary>
/// java.util.ArrayList as the base of a class of the program: C#'s List has no virtual methods, so a subclass could
/// not override add or remove. Java's methods are virtual here, under the names the translation calls them by. It is
/// a List, but a call through a List-typed reference reaches List's method, not the subclass's override.
/// </summary>
/// <summary>
/// java.lang.Number as a base class: Java's abstract values, and C#'s IConvertible on them, as a boxed number is one, so
/// that Convert.ToInt32(n) reads a Number of the program as it does an Integer.
/// </summary>
public abstract class JavaNumber : IConvertible
{
    public abstract int IntValue();

    public abstract long LongValue();

    public abstract float FloatValue();

    public abstract double DoubleValue();

    public virtual short ShortValue() => (short) IntValue();

    public virtual sbyte ByteValue() => (sbyte) IntValue();

    TypeCode IConvertible.GetTypeCode() => TypeCode.Object;

    bool IConvertible.ToBoolean(IFormatProvider? provider) => LongValue() != 0;

    char IConvertible.ToChar(IFormatProvider? provider) => (char) IntValue();

    sbyte IConvertible.ToSByte(IFormatProvider? provider) => ByteValue();

    byte IConvertible.ToByte(IFormatProvider? provider) => (byte) IntValue();

    short IConvertible.ToInt16(IFormatProvider? provider) => ShortValue();

    ushort IConvertible.ToUInt16(IFormatProvider? provider) => (ushort) IntValue();

    int IConvertible.ToInt32(IFormatProvider? provider) => IntValue();

    uint IConvertible.ToUInt32(IFormatProvider? provider) => (uint) LongValue();

    long IConvertible.ToInt64(IFormatProvider? provider) => LongValue();

    ulong IConvertible.ToUInt64(IFormatProvider? provider) => (ulong) LongValue();

    float IConvertible.ToSingle(IFormatProvider? provider) => FloatValue();

    double IConvertible.ToDouble(IFormatProvider? provider) => DoubleValue();

    decimal IConvertible.ToDecimal(IFormatProvider? provider) => (decimal) DoubleValue();

    DateTime IConvertible.ToDateTime(IFormatProvider? provider) => throw new InvalidCastException("Number to DateTime");

    string IConvertible.ToString(IFormatProvider? provider) => ToString() ?? "";

    object IConvertible.ToType(Type conversionType, IFormatProvider? provider) =>
        Convert.ChangeType(DoubleValue(), conversionType, provider);
}

/// <summary>
/// java.util.AbstractSet as a base class: C#'s ISet has no virtual methods, so a class extending AbstractSet extends
/// this, whose Java methods are abstract (Iterator, Size) or virtual, as AbstractSet and AbstractCollection have
/// them, and which is C#'s ISet on them.
/// </summary>
public abstract class JavaSet<E> : ISet<E>, IReadOnlyCollection<E>
{
    public abstract IJavaIterator<E> Iterator();

    public abstract int Size();

    public virtual bool IsEmpty() => Size() == 0;

    public virtual bool Contains(object o)
    {
        var it = Iterator();
        while (it.HasNext())
            if (Equals(it.Next(), o)) return true;
        return false;
    }

    public virtual bool Add(E e) => throw new NotSupportedException("add");

    public virtual bool Remove(object o)
    {
        var it = Iterator();
        while (it.HasNext())
        {
            if (!Equals(it.Next(), o)) continue;
            it.Remove();
            return true;
        }
        return false;
    }

    public virtual void Clear()
    {
        var it = Iterator();
        while (it.HasNext())
        {
            it.Next();
            it.Remove();
        }
    }

    public virtual bool ContainsAll(System.Collections.ICollection c) => c.Cast<object>().All(Contains);

    public virtual bool AddAll(ICollection<E> c)
    {
        var changed = false;
        foreach (var e in c) changed |= Add(e);
        return changed;
    }

    public virtual bool RemoveAll(System.Collections.ICollection c)
    {
        var changed = false;
        foreach (var o in c) changed |= Remove(o!);
        return changed;
    }

    public virtual bool RetainAll(System.Collections.ICollection c)
    {
        var keep = c.Cast<object?>().ToList();
        var changed = false;
        var it = Iterator();
        while (it.HasNext())
        {
            if (keep.Contains(it.Next())) continue;
            it.Remove();
            changed = true;
        }
        return changed;
    }

    public virtual object[] ToArray() => this.Select(e => (object) e!).ToArray();

    public IEnumerator<E> GetEnumerator() => Iterator().AsEnumerator();

    System.Collections.IEnumerator System.Collections.IEnumerable.GetEnumerator() => GetEnumerator();

    int ICollection<E>.Count => Size();

    int IReadOnlyCollection<E>.Count => Size();

    bool ICollection<E>.IsReadOnly => false;

    void ICollection<E>.Add(E item) => Add(item);

    bool ISet<E>.Add(E item) => Add(item);

    bool ICollection<E>.Contains(E item) => Contains(item!);

    bool ICollection<E>.Remove(E item) => Remove(item!);

    void ICollection<E>.CopyTo(E[] array, int arrayIndex)
    {
        foreach (var e in this) array[arrayIndex++] = e;
    }

    void ISet<E>.UnionWith(IEnumerable<E> other)
    {
        foreach (var e in other) Add(e);
    }

    void ISet<E>.IntersectWith(IEnumerable<E> other)
    {
        var keep = new HashSet<E>(other);
        var it = Iterator();
        while (it.HasNext())
            if (!keep.Contains(it.Next())) it.Remove();
    }

    void ISet<E>.ExceptWith(IEnumerable<E> other)
    {
        foreach (var e in other) Remove(e!);
    }

    void ISet<E>.SymmetricExceptWith(IEnumerable<E> other)
    {
        foreach (var e in new HashSet<E>(other))
            if (!Remove(e!)) Add(e);
    }

    bool ISet<E>.IsSubsetOf(IEnumerable<E> other) => new HashSet<E>(this).IsSubsetOf(other);

    bool ISet<E>.IsSupersetOf(IEnumerable<E> other) => new HashSet<E>(this).IsSupersetOf(other);

    bool ISet<E>.IsProperSubsetOf(IEnumerable<E> other) => new HashSet<E>(this).IsProperSubsetOf(other);

    bool ISet<E>.IsProperSupersetOf(IEnumerable<E> other) => new HashSet<E>(this).IsProperSupersetOf(other);

    bool ISet<E>.Overlaps(IEnumerable<E> other) => other.Any(e => Contains(e!));

    bool ISet<E>.SetEquals(IEnumerable<E> other) => new HashSet<E>(this).SetEquals(other);
}

public class JavaArrayList<E> : List<E>
{
    public JavaArrayList()
    {
    }

    public JavaArrayList(int capacity) : base(capacity)
    {
    }

    public JavaArrayList(IEnumerable<E> items) : base(items)
    {
    }

    public new virtual bool Add(E item)
    {
        base.Add(item);
        return true;
    }

    public virtual void Add(int index, E item) => Insert(index, item);

    public virtual bool AddAll(ICollection<E> items)
    {
        AddRange(items);
        return items.Count > 0;
    }

    public virtual bool Remove(object? item) => item is E e && base.Remove(e);

    public virtual E Remove(int index)
    {
        var old = this[index];
        RemoveAt(index);
        return old;
    }

    /// <summary>
    /// List.remove(int) on a List: the removal itself, which base.RemoveAtAndGet of an override needs. The translation
    /// calls Remove(int) on a receiver of a subclass, to reach its override.
    /// </summary>
    public E RemoveAtAndGet(int index)
    {
        var old = this[index];
        RemoveAt(index);
        return old;
    }

    public virtual E Set(int index, E item)
    {
        var old = this[index];
        this[index] = item;
        return old;
    }

    public new virtual void Clear() => base.Clear();

    public virtual JavaArrayList<E> Clone() => new JavaArrayList<E>(this);
}

/// <summary>
/// CompletableFuture's continuations, on tasks. A failed task's exception reaches the continuation's task as it is,
/// as Java wraps it once (GetResult, where Result would wrap it in a second AggregateException).
/// </summary>
public static class JavaTasks
{
    public static Task<R> ThenApply<T, R>(this Task<T> t, Func<T, R> f) => t.ContinueWith(x => f(x.GetAwaiter().GetResult()));

    public static Task<R> ThenApply<R>(this Task t, Func<object?, R> f) => t.ContinueWith(x =>
    {
        x.GetAwaiter().GetResult();
        return f(null);
    });

    public static Task<R> ThenCompose<T, R>(this Task<T> t, Func<T, Task<R>> f) =>
        t.ContinueWith(x => f(x.GetAwaiter().GetResult())).Unwrap();

    public static Task<R> ThenCompose<R>(this Task t, Func<object?, Task<R>> f) => t.ContinueWith(x =>
    {
        x.GetAwaiter().GetResult();
        return f(null);
    }).Unwrap();

    public static Task ThenAccept<T>(this Task<T> t, Action<T> a) => t.ContinueWith(x => a(x.GetAwaiter().GetResult()));
}

/// <summary>java.util.concurrent.locks.Lock: what a ReentrantLock and a read-write lock's halves are.</summary>
public interface IJavaLock
{
    void Lock();

    void Unlock();

    bool TryLock();
}

public class JavaReentrantLock : IJavaLock
{
    private readonly object monitor = new();

    public JavaReentrantLock()
    {
    }

    public JavaReentrantLock(bool fair)
    {
    }

    public void Lock() => Monitor.Enter(monitor);

    public void Unlock() => Monitor.Exit(monitor);

    public bool TryLock() => Monitor.TryEnter(monitor);
}

/// <summary>ReadWriteLock.readLock() and writeLock() of a ReaderWriterLockSlim.</summary>
public static class JavaLocks
{
    public static IJavaLock ReadLock(this ReaderWriterLockSlim rw) => new Half(rw, false);

    public static IJavaLock WriteLock(this ReaderWriterLockSlim rw) => new Half(rw, true);

    private sealed class Half(ReaderWriterLockSlim rw, bool write) : IJavaLock
    {
        public void Lock()
        {
            if (write) rw.EnterWriteLock();
            else rw.EnterReadLock();
        }

        public void Unlock()
        {
            if (write) rw.ExitWriteLock();
            else rw.ExitReadLock();
        }

        public bool TryLock() => write ? rw.TryEnterWriteLock(0) : rw.TryEnterReadLock(0);
    }
}

/// <summary>java.util.concurrent.atomic: a volatile value with Interlocked updates.</summary>
public class AtomicBoolean
{
    private int value;

    public AtomicBoolean() { }

    public AtomicBoolean(bool initial) => value = initial ? 1 : 0;

    public bool Get() => Volatile.Read(ref value) != 0;

    public void Set(bool v) => Volatile.Write(ref value, v ? 1 : 0);

    public bool GetAndSet(bool v) => Interlocked.Exchange(ref value, v ? 1 : 0) != 0;

    public bool CompareAndSet(bool expected, bool update) =>
        Interlocked.CompareExchange(ref value, update ? 1 : 0, expected ? 1 : 0) == (expected ? 1 : 0);

    public override string ToString() => Get() ? "true" : "false";
}

public class AtomicInteger
{
    private int value;

    public AtomicInteger() { }

    public AtomicInteger(int initial) => value = initial;

    public int Get() => Volatile.Read(ref value);

    public void Set(int v) => Volatile.Write(ref value, v);

    public int GetAndSet(int v) => Interlocked.Exchange(ref value, v);

    public bool CompareAndSet(int expected, int update) => Interlocked.CompareExchange(ref value, update, expected) == expected;

    public int IncrementAndGet() => Interlocked.Increment(ref value);

    public int DecrementAndGet() => Interlocked.Decrement(ref value);

    public int GetAndIncrement() => Interlocked.Increment(ref value) - 1;

    public int GetAndDecrement() => Interlocked.Decrement(ref value) + 1;

    public int AddAndGet(int delta) => Interlocked.Add(ref value, delta);

    public int GetAndAdd(int delta) => Interlocked.Add(ref value, delta) - delta;

    public double DoubleValue() => Get();

    public long LongValue() => Get();

    public override string ToString() => Get().ToString();
}

public class AtomicLong
{
    private long value;

    public AtomicLong() { }

    public AtomicLong(long initial) => value = initial;

    public long Get() => Interlocked.Read(ref value);

    public void Set(long v) => Interlocked.Exchange(ref value, v);

    public long GetAndSet(long v) => Interlocked.Exchange(ref value, v);

    public bool CompareAndSet(long expected, long update) =>
        Interlocked.CompareExchange(ref value, update, expected) == expected;

    public long IncrementAndGet() => Interlocked.Increment(ref value);

    public long DecrementAndGet() => Interlocked.Decrement(ref value);

    public long GetAndIncrement() => Interlocked.Increment(ref value) - 1;

    public long AddAndGet(long delta) => Interlocked.Add(ref value, delta);

    public long GetAndAdd(long delta) => Interlocked.Add(ref value, delta) - delta;

    public double DoubleValue() => Get();

    public override string ToString() => Get().ToString();
}

/// <summary>An AtomicReference may hold a value type (a boxed Integer is int?): a lock, not Interlocked.</summary>
public class AtomicReference<T>
{
    private readonly object monitor = new();
    private T value;

    // null, as Java's; T is nullable when the program's reference may be
    public AtomicReference() => value = default!;

    public AtomicReference(T initial) => value = initial;

    public T Get()
    {
        lock (monitor) return value;
    }

    public void Set(T v)
    {
        lock (monitor) value = v;
    }

    public T GetAndSet(T v)
    {
        lock (monitor)
        {
            var old = value;
            value = v;
            return old;
        }
    }

    /// <summary>Java compares references; a value type has no identity, its value is compared.</summary>
    public bool CompareAndSet(T expected, T update)
    {
        lock (monitor)
        {
            bool same = typeof(T).IsValueType ? EqualityComparer<T>.Default.Equals(value, expected)
                : ReferenceEquals(value, expected);
            if (same) value = update;
            return same;
        }
    }

    public override string ToString() => Get()?.ToString() ?? "null";
}

/// <summary>java.lang.reflect.WildcardType: .NET's reflection has no wildcards, so no Type is one.</summary>
public interface IJavaWildcardType
{
    Type[] GetUpperBounds();

    Type[] GetLowerBounds();
}

/// <summary>Class.getModifiers and its kin: java.lang.reflect.Modifier's bits of a type or a member.</summary>
public static class JavaModifier
{
    public const int Public = 0x0001, Private = 0x0002, Protected = 0x0004, Static = 0x0008, Final = 0x0010,
        Interface = 0x0200, Abstract = 0x0400;

    public static int Of(Type t) =>
        (t.IsPublic || t.IsNestedPublic ? Public : 0)
        | (t.IsNestedPrivate ? Private : 0)
        | (t.IsNestedFamily ? Protected : 0)
        | (t.IsAbstract && t.IsSealed ? Static : 0)
        | (t.IsSealed && !t.IsAbstract ? Final : 0)
        | (t.IsInterface ? Interface | Abstract : 0)
        | (t.IsAbstract && !t.IsSealed && !t.IsInterface ? Abstract : 0);

    public static int Of(MethodBase m) =>
        (m.IsPublic ? Public : 0) | (m.IsPrivate ? Private : 0) | (m.IsFamily ? Protected : 0)
        | (m.IsStatic ? Static : 0) | (m.IsFinal ? Final : 0) | (m.IsAbstract ? Abstract : 0);

    public static int Of(FieldInfo f) =>
        (f.IsPublic ? Public : 0) | (f.IsPrivate ? Private : 0) | (f.IsFamily ? Protected : 0)
        | (f.IsStatic ? Static : 0) | (f.IsInitOnly || f.IsLiteral ? Final : 0);
}

/// <summary>
/// java.util.ServiceLoader: the implementations of a service are the concrete classes with a public parameterless
/// constructor in the loaded assemblies (or in the one given), instantiated as they are enumerated.
/// </summary>
public static class JavaServiceLoader
{
    public static IEnumerable<T> Load<T>(Assembly? assembly = null)
    {
        var assemblies = assembly != null ? new[] { assembly } : AppDomain.CurrentDomain.GetAssemblies();
        foreach (var a in assemblies)
        {
            Type[] types;
            try
            {
                types = a.GetTypes();
            }
            catch (ReflectionTypeLoadException e)
            {
                types = e.Types.OfType<Type>().ToArray();
            }
            foreach (var t in types)
            {
                if (t.IsAbstract || t.IsInterface || !typeof(T).IsAssignableFrom(t) || t.ContainsGenericParameters
                    || t.GetConstructor(Type.EmptyTypes) == null)
                {
                    continue;
                }
                yield return (T) Activator.CreateInstance(t)!;
            }
        }
    }
}

/// <summary>
/// Java's resources, Class.getResource and ClassLoader.getResourceAsStream, as an assembly's embedded resources. A
/// resource is found by its Java path: the manifest name that is the path (an EmbeddedResource with that LogicalName),
/// or that ends with it, with dots for slashes as MSBuild names them. A name without a leading slash is relative to
/// the class's package; of several matches, the one whose folders are most like the class's namespace wins. Its URL is
/// resource://assembly/manifest-name, which OpenStream reads, as it reads a file: URI.
/// </summary>
public static class JavaResources
{
    public const string Scheme = "resource";

    public static Uri? GetResource(Type type, string name) => Uri(type.Assembly, Find(type.Assembly, name, type.Namespace));

    public static Stream? GetResourceAsStream(Type type, string name) =>
        Open(type.Assembly, Find(type.Assembly, name, type.Namespace));

    /// <summary>A class loader's name is absolute, without its leading slash.</summary>
    public static Uri? GetResource(Assembly assembly, string name) => Uri(assembly, Find(assembly, "/" + name, null));

    public static Stream? GetResourceAsStream(Assembly assembly, string name) => Open(assembly, Find(assembly, "/" + name, null));

    public static Stream OpenStream(Uri uri)
    {
        if (uri.Scheme == Scheme)
        {
            var assembly = AppDomain.CurrentDomain.GetAssemblies()
                .FirstOrDefault(a => string.Equals(a.GetName().Name, System.Uri.UnescapeDataString(uri.Host),
                    StringComparison.OrdinalIgnoreCase)); // a URI's host is lower case
            var stream = assembly?.GetManifestResourceStream(System.Uri.UnescapeDataString(uri.AbsolutePath.TrimStart('/')));
            return stream ?? throw new FileNotFoundException(uri.ToString());
        }
        if (uri.IsFile) return File.OpenRead(uri.LocalPath);
        throw new NotSupportedException("reading " + uri);
    }

    private static Uri? Uri(Assembly assembly, string? manifestName) => manifestName == null ? null
        : new Uri(Scheme + "://" + System.Uri.EscapeDataString(assembly.GetName().Name!) + "/"
                  + System.Uri.EscapeDataString(manifestName));

    private static Stream? Open(Assembly assembly, string? manifestName) =>
        manifestName == null ? null : assembly.GetManifestResourceStream(manifestName);

    private static string? Find(Assembly assembly, string name, string? near)
    {
        var path = name.TrimStart('/');
        var dotted = path.Replace('/', '.');
        var candidates = assembly.GetManifestResourceNames()
            .Where(m => m == path || m.EndsWith("/" + path, StringComparison.Ordinal)
                        || m == dotted || m.EndsWith("." + dotted, StringComparison.Ordinal))
            .ToList();
        if (candidates.Count <= 1 || near == null || name.StartsWith('/'))
        {
            // an absolute name: the shortest match, the one closest to the root
            return candidates.OrderBy(m => m.Length).FirstOrDefault();
        }
        var segments = near.ToLowerInvariant().Split('.');
        return candidates.OrderByDescending(m => m.ToLowerInvariant().Split('.', '/').Count(s => segments.Contains(s)))
            .First();
    }
}
