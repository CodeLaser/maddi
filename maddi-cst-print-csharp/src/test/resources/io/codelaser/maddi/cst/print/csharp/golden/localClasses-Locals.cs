using System.Collections.Generic;
namespace Org.Example.Local;
internal sealed class Locals {
    private int @base = 1;
    internal int Count(int n) {
        List<Entry> entries = new List<Entry>();
        for (int i = 0; i < n; i++) { entries.Add(new Entry(i)); }
        return entries.Count;
    }
    internal int Captures(int n) { return new Adder().Add(1); }
    private sealed class Entry {
        internal readonly int value;
        internal Entry(int value) { this.value = value; }
    }
}
