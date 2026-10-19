using Maddi.JavaCompat;
using System;
using System.Collections.Generic;
using System.Linq;
namespace Org.Example.Refses;
internal sealed class Refs {
    internal static readonly Comparison<Location> ORDER = JavaComparator.Comparing<Location>((Location p0) => p0
        .ClassName).ThenComparing((Location p0) => p0.LineNumber);

    internal static List<string> Names(List<Location> locations) => locations.Select((Location p0) => p0.ClassName)
        .ToList();
    internal static Func<string> Bound(Location location) => () => location.ClassName;
    internal sealed record Location(string ClassName, int LineNumber);
}
