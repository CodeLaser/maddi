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

#nullable disable

using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;

namespace Maddi.JavaCompat;

/// <summary>Java's collection methods whose results the BCL's do not have.</summary>
public static class JavaCollections
{
    /// <summary>Map.put: the previous value, or the default when there was none.</summary>
    public static V Put<K, V>(this IDictionary<K, V> d, K key, V value)
    {
        d.TryGetValue(key, out var old);
        d[key] = value;
        return old;
    }

    /// <summary>Map.get of a map whose values are a value type: null when the key is absent, as in Java.</summary>
    public static V? GetValueOrNull<K, V>(this IDictionary<K, V> d, K key) where V : struct =>
        d.TryGetValue(key, out var v) ? v : null;

    /// <summary>Map.get on the IDictionary interface: the default when the key is absent.</summary>
    public static V Get<K, V>(this IDictionary<K, V> d, K key) => d.TryGetValue(key, out var v) ? v : default;

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

    /// <summary>Iterable.iterator: Java's iterator, with remove.</summary>
    public static JavaIterator<T> Iterator<T>(this IEnumerable<T> e) => new JavaIterator<T>(e);

    public static V RemoveAndGet<K, V>(this IDictionary<K, V> d, K key)
    {
        if (!d.TryGetValue(key, out var old)) return default;
        d.Remove(key);
        return old;
    }

    public static void PutAll<K, V>(this IDictionary<K, V> d, IEnumerable<KeyValuePair<K, V>> other)
    {
        foreach (var (k, v) in other.ToList()) d[k] = v;
    }

    public static V PutIfAbsent<K, V>(this IDictionary<K, V> d, K key, V value)
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

    public static V Compute<K, V>(this IDictionary<K, V> d, K key, Func<K, V, V> f)
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

    public static bool InsertAll<T>(this List<T> l, int index, IEnumerable<T> items)
    {
        var all = items.ToList();
        l.InsertRange(index, all);
        return all.Count > 0;
    }

    public static bool RetainAll<T>(this ICollection<T> c, IEnumerable<T> keep)
    {
        var kept = new HashSet<T>(keep);
        var removed = c.Where(x => !kept.Contains(x)).ToList();
        foreach (var x in removed) c.Remove(x);
        return removed.Count > 0;
    }

    /// <summary>List.remove(int): the element removed.</summary>
    public static T RemoveAtAndGet<T>(this List<T> l, int index)
    {
        var x = l[index];
        l.RemoveAt(index);
        return x;
    }

    /// <summary>List.set: the element replaced.</summary>
    public static T Set<T>(this List<T> l, int index, T value)
    {
        var old = l[index];
        l[index] = value;
        return old;
    }

    public static T RemoveFirst<T>(this List<T> l)
    {
        if (l.Count == 0) throw new InvalidOperationException("empty");
        return l.RemoveAtAndGet(0);
    }

    public static T RemoveLast<T>(this List<T> l)
    {
        if (l.Count == 0) throw new InvalidOperationException("empty");
        return l.RemoveAtAndGet(l.Count - 1);
    }

    public static T PeekFirst<T>(this List<T> l) => l.Count == 0 ? default : l[0];

    public static T PeekLast<T>(this List<T> l) => l.Count == 0 ? default : l[^1];

    public static T PollFirst<T>(this List<T> l) => l.Count == 0 ? default : l.RemoveAtAndGet(0);

    public static T PollLast<T>(this List<T> l) => l.Count == 0 ? default : l.RemoveAtAndGet(l.Count - 1);

    public static bool Offer<T>(this List<T> l, T x)
    {
        l.Add(x);
        return true;
    }

    /// <summary>Iterable.forEach on anything enumerable; a List's own ForEach takes precedence.</summary>
    public static void ForEach<T>(this IEnumerable<T> e, Action<T> action)
    {
        foreach (var x in e) action(x);
    }

    public static IEnumerable<T> Sorted<T>(this IEnumerable<T> e, Comparison<T> comparison) =>
        e.Order(Comparer<T>.Create(comparison));

    // Optional<T> is the value itself, or null
    public static void IfPresent<T>(this T value, Action<T> action)
    {
        if (value != null) action(value);
    }

    public static R Map<T, R>(this T value, Func<T, R> f) => value == null ? default : f(value);

    public static T Filter<T>(this T value, Func<T, bool> p) => value != null && p(value) ? value : default;
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
}

/// <summary>Java's String methods that differ from the BCL's.</summary>
public static class JavaString
{
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

    public static sbyte[] GetBytes(string s, Encoding encoding) => (sbyte[])(object)encoding.GetBytes(s);

    public static string ValueOf(object o) => o switch
    {
        null => "null",
        bool b => ValueOf(b),
        double d => ValueOf(d),
        float f => ValueOf(f),
        char[] a => new string(a),
        _ => o.ToString()
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

    /// <summary>String.format: Java's %-conversions (s, d, x, X, c, b, f, n, %), with flags, width and precision.</summary>
    public static string Format(string format, params object[] args)
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
                'x' => Convert.ToString(Convert.ToInt64(arg, CultureInfo.InvariantCulture), 16),
                'X' => Convert.ToString(Convert.ToInt64(arg, CultureInfo.InvariantCulture), 16).ToUpperInvariant(),
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
                text = flags.Contains('-') ? text.PadRight(width)
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
    public static bool IsJavaIdentifierStart(int c)
    {
        if (c == '$' || c == '_') return true;
        var category = CharUnicodeInfo.GetUnicodeCategory(c);
        return char.IsLetter((char)c) || category is UnicodeCategory.LetterNumber
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
    public static long Round(double d) => (long)Math.Floor(d + 0.5);

    public static int Round(float f) => (int)MathF.Floor(f + 0.5f);

    public static int FloorMod(int x, int y) => ((x % y) + y) % y;

    public static long FloorMod(long x, long y) => ((x % y) + y) % y;

    public static int FloorDiv(int x, int y) => (int)Math.Floor((double)x / y);

    public static long FloorDiv(long x, long y) => x / y - ((x % y != 0 && (x ^ y) < 0) ? 1 : 0);
}

public static class JavaArrays
{
    public static T[] CopyOf<T>(T[] a, int length)
    {
        var copy = new T[length];
        Array.Copy(a, copy, Math.Min(length, a.Length));
        return copy;
    }

    public static bool Equals<T>(T[] a, T[] b) =>
        ReferenceEquals(a, b) || a != null && b != null && a.Length == b.Length
        && a.Zip(b).All(p => object.Equals(p.First, p.Second));

    public static int HashCode<T>(T[] a)
    {
        if (a == null) return 0;
        var h = 1;
        foreach (var x in a) h = 31 * h + (x?.GetHashCode() ?? 0);
        return h;
    }

    public static string ToString<T>(T[] a) =>
        a == null ? "null" : "[" + string.Join(", ", a.Select(x => JavaString.ValueOf(x))) + "]";
}

public static class JavaSystem
{
    public static string GetProperty(string key, string def = null) => key switch
    {
        "line.separator" => Environment.NewLine,
        "file.separator" => Path.DirectorySeparatorChar.ToString(),
        "path.separator" => Path.PathSeparator.ToString(),
        "user.dir" => Environment.CurrentDirectory,
        "user.home" => Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
        "java.io.tmpdir" => Path.GetTempPath(),
        "os.name" => System.Runtime.InteropServices.RuntimeInformation.OSDescription,
        _ => def
    };
}

/// <summary>java.io.ByteArrayInputStream: a MemoryStream over a Java byte[].</summary>
public class ByteArrayInputStream : MemoryStream
{
    public ByteArrayInputStream(sbyte[] bytes) : base((byte[])(object)bytes, false)
    {
    }

    public ByteArrayInputStream(sbyte[] bytes, int offset, int length) : base((byte[])(object)bytes, offset, length, false)
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

    public sbyte[] ToByteArray() => (sbyte[])(object)ToArray();

    public void Write(int b) => WriteByte((byte)b);

    public void Write(sbyte[] b, int offset, int length) => Write((byte[])(object)b, offset, length);

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
        var n = input.Read((byte[])(object)b, offset, length);
        return n == 0 && length > 0 ? -1 : n;
    }

    public void ReadFully(sbyte[] b) => ReadFully(b, 0, b.Length);

    public void ReadFully(sbyte[] b, int offset, int length) => input.ReadExactly((byte[])(object)b, offset, length);

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

    public override bool Equals(object obj)
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
public class JavaIterator<T>
{
    private readonly IList<T> list;
    private readonly ICollection<T> collection;
    private readonly IEnumerator<T> enumerator;
    private int index;
    private bool hasPeeked;
    private bool peeked;
    private T last;

    public JavaIterator(IEnumerable<T> source)
    {
        list = source as IList<T>;
        if (list == null)
        {
            collection = source as ICollection<T>;
            enumerator = (collection != null ? source.ToList() : source).GetEnumerator();
        }
    }

    public bool HasNext()
    {
        if (list != null) return index < list.Count;
        if (!hasPeeked)
        {
            peeked = enumerator.MoveNext();
            hasPeeked = true;
        }
        return peeked;
    }

    public T Next()
    {
        if (!HasNext()) throw new InvalidOperationException("no next element");
        if (list != null) return last = list[index++];
        hasPeeked = false;
        return last = enumerator.Current;
    }

    public void Remove()
    {
        if (list != null) list.RemoveAt(--index);
        else if (collection != null) collection.Remove(last);
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

    public JavaFile(string parent, string child) => path = parent == null ? child : Path.Combine(parent, child);

    public JavaFile(JavaFile parent, string child) : this(parent?.path, child)
    {
    }

    public string GetPath() => path;

    public string GetName() => Path.GetFileName(path);

    public string GetAbsolutePath() => Path.GetFullPath(path);

    public JavaFile GetAbsoluteFile() => new JavaFile(GetAbsolutePath());

    public string GetCanonicalPath() => Path.GetFullPath(path);

    public JavaFile GetCanonicalFile() => new JavaFile(GetCanonicalPath());

    public string GetParent() => Path.GetDirectoryName(path);

    public JavaFile GetParentFile() => GetParent() is { } p ? new JavaFile(p) : null;

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

    public string[] List() => Directory.Exists(path)
        ? Directory.EnumerateFileSystemEntries(path).Select(Path.GetFileName).ToArray()
        : null;

    public JavaFile[] ListFiles() => Directory.Exists(path)
        ? Directory.EnumerateFileSystemEntries(path).Select(p => new JavaFile(p)).ToArray()
        : null;

    public JavaFile[] ListFiles(Func<JavaFile, bool> filter) => ListFiles()?.Where(filter).ToArray();

    public bool RenameTo(JavaFile dest)
    {
        if (File.Exists(path)) File.Move(path, dest.path);
        else if (Directory.Exists(path)) Directory.Move(path, dest.path);
        else return false;
        return true;
    }

    public bool IsAbsolute() => Path.IsPathRooted(path);

    public static readonly char SeparatorChar = Path.DirectorySeparatorChar;

    public static readonly string Separator = Path.DirectorySeparatorChar.ToString();

    public override string ToString() => path;

    public override bool Equals(object o) => o is JavaFile f && f.path == path;

    public override int GetHashCode() => path.GetHashCode();
}
