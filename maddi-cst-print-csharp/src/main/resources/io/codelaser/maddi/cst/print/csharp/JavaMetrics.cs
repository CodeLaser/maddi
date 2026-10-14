// maddi: the compatibility library's Micrometer, for micrometer-system-diagnostics-metrics.csmap.
//
// Micrometer's global registry over System.Diagnostics.Metrics. A gauge is registered by its name and tags, read back
// (registry.find(name).tags(…).gauge().value()) and removed, as translated code does with Micrometer; .NET's
// instruments have neither, so the registry keeps the gauges and exports each name as an ObservableGauge of the meter
// "Maddi.JavaCompat.Micrometer", which a MeterListener or OpenTelemetry collects. Counters and long task timers are
// that meter's Counter<double> and Histogram<double> (seconds).
#nullable enable
using System;
using System.Collections;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.Diagnostics.Metrics;
using System.Globalization;
using System.Linq;

namespace Maddi.JavaCompat;

/// <summary>Micrometer's Tags: immutable key-value pairs, sorted by key, equal by content (a map key).</summary>
public sealed class JavaTags : IEquatable<JavaTags>, IEnumerable<KeyValuePair<string, string>>
{
    public static readonly JavaTags Empty = new(new SortedDictionary<string, string>(StringComparer.Ordinal));

    private readonly SortedDictionary<string, string> tags;

    private JavaTags(SortedDictionary<string, string> tags) => this.tags = tags;

    public static JavaTags Of(string key, string value) => Empty.And(key, value);

    public JavaTags And(string key, string value)
    {
        var copy = new SortedDictionary<string, string>(tags, StringComparer.Ordinal) { [key] = value };
        return new JavaTags(copy);
    }

    public JavaTags And(JavaTags? other)
    {
        if (other == null || other.tags.Count == 0) return this;
        var copy = new SortedDictionary<string, string>(tags, StringComparer.Ordinal);
        foreach (var (key, value) in other.tags) copy[key] = value;
        return new JavaTags(copy);
    }

    /// <summary>Whether every tag of <paramref name="subset"/> is one of these: a search's match.</summary>
    internal bool ContainsAll(JavaTags subset) =>
        subset.tags.All(t => tags.TryGetValue(t.Key, out var v) && v == t.Value);

    internal KeyValuePair<string, object?>[] ToInstrumentTags() =>
        tags.Select(t => new KeyValuePair<string, object?>(t.Key, t.Value)).ToArray();

    public bool Equals(JavaTags? other) => other != null && tags.SequenceEqual(other.tags);

    public override bool Equals(object? obj) => Equals(obj as JavaTags);

    public override int GetHashCode()
    {
        var hash = new HashCode();
        foreach (var (key, value) in tags)
        {
            hash.Add(key);
            hash.Add(value);
        }
        return hash.ToHashCode();
    }

    public IEnumerator<KeyValuePair<string, string>> GetEnumerator() => tags.GetEnumerator();

    IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();

    public override string ToString() => "[" + string.Join(",", tags.Select(t => "tag(" + t.Key + "=" + t.Value + ")")) + "]";
}

/// <summary>Micrometer's Meter.Type.</summary>
public enum JavaMeterType { Counter, Gauge, LongTaskTimer, Timer, DistributionSummary, Other }

/// <summary>Micrometer's Meter.Id: a meter is its name and tags; the unit, description and type describe it.</summary>
public sealed record JavaMeterId(string Name, JavaTags Tags, string? BaseUnit, string? Description, JavaMeterType Type);

/// <summary>A registered gauge: reads its value when asked, NaN when the reading fails, as Micrometer's.</summary>
public sealed class JavaGauge
{
    private readonly Func<double> read;

    internal JavaGauge(JavaMeterId id, Func<double> read)
    {
        Id = id;
        this.read = read;
    }

    public JavaMeterId Id { get; }

    public double Value()
    {
        try
        {
            return read();
        }
        catch (Exception)
        {
            return double.NaN;
        }
    }
}

/// <summary>Micrometer's MeterRegistry, as far as gauges go: registered, found, removed.</summary>
public sealed class JavaMeterRegistry
{
    private readonly ConcurrentDictionary<(string Name, JavaTags Tags), JavaGauge> gauges = new();

    /// <summary>The gauge of <paramref name="name"/> and <paramref name="tags"/>; the first registration wins.</summary>
    internal JavaGauge Gauge(string name, JavaTags tags, Func<double> read)
    {
        var gauge = gauges.GetOrAdd((name, tags),
            _ => new JavaGauge(new JavaMeterId(name, tags, null, null, JavaMeterType.Gauge), read));
        if (ReferenceEquals(this, JavaMetrics.GlobalRegistry)) JavaMetrics.Export(name);
        return gauge;
    }

    internal IEnumerable<Measurement<double>> Measure(string name) =>
        gauges.Where(g => g.Key.Name == name)
            .Select(g => new Measurement<double>(g.Value.Value(), g.Key.Tags.ToInstrumentTags()));

    public JavaGauge? Remove(JavaMeterId id) => gauges.TryRemove((id.Name, id.Tags), out var removed) ? removed : null;

    public JavaMeterSearch Find(string name) => new(this, name, JavaTags.Empty);

    internal JavaGauge? FindGauge(string name, JavaTags tags) =>
        gauges.Where(g => g.Key.Name == name && g.Key.Tags.ContainsAll(tags)).Select(g => g.Value).FirstOrDefault();
}

/// <summary>Micrometer's Search: a name, narrowed by tags.</summary>
public sealed class JavaMeterSearch
{
    private readonly JavaMeterRegistry registry;
    private readonly string name;
    private readonly JavaTags tags;

    internal JavaMeterSearch(JavaMeterRegistry registry, string name, JavaTags tags)
    {
        this.registry = registry;
        this.name = name;
        this.tags = tags;
    }

    public JavaMeterSearch Tags(JavaTags more) => new(registry, name, tags.And(more));

    public JavaGauge? Gauge() => registry.FindGauge(name, tags);
}

/// <summary>Micrometer's Counter.</summary>
public sealed class JavaCounter
{
    private readonly Counter<double> counter;
    private readonly KeyValuePair<string, object?>[] tags;

    internal JavaCounter(Counter<double> counter, KeyValuePair<string, object?>[] tags)
    {
        this.counter = counter;
        this.tags = tags;
    }

    public void Increment() => counter.Add(1, tags);
}

/// <summary>Micrometer's LongTaskTimer: each sample's duration is recorded when it stops.</summary>
public sealed class JavaLongTaskTimer
{
    private readonly Histogram<double> durations;
    private readonly KeyValuePair<string, object?>[] tags;

    internal JavaLongTaskTimer(Histogram<double> durations, KeyValuePair<string, object?>[] tags)
    {
        this.durations = durations;
        this.tags = tags;
    }

    public Sample Start() => new(this, Stopwatch.GetTimestamp());

    public sealed class Sample
    {
        private readonly JavaLongTaskTimer timer;
        private readonly long start;

        internal Sample(JavaLongTaskTimer timer, long start)
        {
            this.timer = timer;
            this.start = start;
        }

        /// <summary>Records the duration; returns it in nanoseconds, as Micrometer's.</summary>
        public long Stop()
        {
            var elapsed = Stopwatch.GetElapsedTime(start);
            timer.durations.Record(elapsed.TotalSeconds, timer.tags);
            return (long) elapsed.TotalNanoseconds;
        }
    }
}

/// <summary>Micrometer's Metrics.more().</summary>
public sealed class JavaMoreMetrics
{
    public JavaLongTaskTimer LongTaskTimer(string name, params string[] tags) =>
        new(JavaMetrics.Histogram(name), JavaMetrics.PairTags(tags));
}

/// <summary>Micrometer's JvmMemoryMetrics, as .NET has it: the managed heap in use.</summary>
public sealed class JavaMemoryMetrics
{
    private readonly JavaTags tags;

    public JavaMemoryMetrics(JavaTags tags) => this.tags = tags;

    public void BindTo(JavaMeterRegistry registry)
    {
        registry.Gauge("jvm.memory.used", tags, () => GC.GetTotalMemory(false));
        registry.Gauge("jvm.memory.committed", tags, () => GC.GetGCMemoryInfo().TotalCommittedBytes);
    }
}

/// <summary>Micrometer's Metrics: the global registry, and its static gauge, counter and timer.</summary>
public static class JavaMetrics
{
    public static readonly Meter Meter = new("Maddi.JavaCompat.Micrometer");

    public static readonly JavaMeterRegistry GlobalRegistry = new();

    public static readonly JavaMoreMetrics More = new();

    private static readonly ConcurrentDictionary<string, ObservableGauge<double>> Exported = new();
    private static readonly ConcurrentDictionary<string, Counter<double>> Counters = new();
    private static readonly ConcurrentDictionary<string, Histogram<double>> Histograms = new();

    /// <summary>
    /// gauge(name, tags, obj, f): <paramref name="obj"/>, whose gauge reads f(obj). As Micrometer's, the gauge does
    /// not keep the object alive: once it is collected, the gauge reads NaN. A value type's copy is kept.
    /// </summary>
    public static T Gauge<T>(string name, JavaTags tags, T obj, Func<T, double> value)
    {
        GlobalRegistry.Gauge(name, tags, Weakly(obj, value));
        return obj;
    }

    /// <summary>gauge(name, obj, f), without tags.</summary>
    public static T Gauge<T>(string name, T obj, Func<T, double> value) => Gauge(name, JavaTags.Empty, obj, value);

    /// <summary>gauge(name, tags, number): <paramref name="number"/>, whose gauge reads its value, weakly too.</summary>
    public static T Gauge<T>(string name, JavaTags tags, T number)
    {
        GlobalRegistry.Gauge(name, tags, Weakly(number, n => NumberValue(n)));
        return number;
    }

    private static Func<double> Weakly<T>(T obj, Func<T, double> value) =>
        obj is null || typeof(T).IsValueType ? Strongly(obj, value) : Weakly(new WeakReference<object>(obj), value);

    // separate methods: one method's lambdas share the closure, which would hold obj
    private static Func<double> Strongly<T>(T obj, Func<T, double> value) => () => value(obj);

    private static Func<double> Weakly<T>(WeakReference<object> weak, Func<T, double> value) =>
        () => weak.TryGetTarget(out var target) ? value((T)target) : double.NaN;

    public static JavaCounter Counter(string name, params string[] tags) =>
        new(Counters.GetOrAdd(name, n => Meter.CreateCounter<double>(n)), PairTags(tags));

    internal static Histogram<double> Histogram(string name) =>
        Histograms.GetOrAdd(name, n => Meter.CreateHistogram<double>(n, "s"));

    internal static void Export(string name) =>
        Exported.GetOrAdd(name, n => Meter.CreateObservableGauge(n, () => GlobalRegistry.Measure(n)));

    /// <summary>Micrometer's tags as varargs: key, value, key, value, ….</summary>
    internal static KeyValuePair<string, object?>[] PairTags(string[] keysAndValues)
    {
        var pairs = new KeyValuePair<string, object?>[keysAndValues.Length / 2];
        for (var i = 0; i < pairs.Length; i++)
        {
            pairs[i] = new KeyValuePair<string, object?>(keysAndValues[2 * i], keysAndValues[2 * i + 1]);
        }
        return pairs;
    }

    private static double NumberValue(object? number) => number switch
    {
        AtomicInteger i => i.Get(),
        AtomicLong l => l.Get(),
        IConvertible c => c.ToDouble(CultureInfo.InvariantCulture),
        _ => double.NaN,
    };
}
