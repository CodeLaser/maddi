using System;
using System.Collections;
using System.Collections.Generic;
namespace Org.Example.Unbound;
internal sealed class Checks {
    internal static T EnsureNotEmpty<T>(T c) where T : ICollection {
        if (c.Count == 0) { throw new ArgumentException(); }
        return c;
    }
    internal static int Size(IList list) => list.Count;
    internal static List<string> Names(List<string> names) => EnsureNotEmpty(names);
}
