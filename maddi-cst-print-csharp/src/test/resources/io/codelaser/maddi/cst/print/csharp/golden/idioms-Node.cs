using System;
using System.Collections.Generic;
namespace Org.Example.Idiom;
internal sealed class Node {
    internal int value;
    public Node Clone() {
        try { return (Node) base.MemberwiseClone(); } catch (NotSupportedException e) {
            throw new Exception(e?.ToString(), e);
        }
    }

    internal static int Sum(List<int> values) {
        int total = 0;
        foreach (int? vItem in values) {
            int? v = vItem;
            if (v == null) { v = 0; }
            total += (int) v;
        }
        return total;
    }
}
