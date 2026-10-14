using System;
namespace Org.Example.Expr;
internal sealed class Expressions {
    internal static int Twice(Op op, int x) => op(x, x);
    internal static int Add(int a, int b) => a + b;
    internal static bool Same(string a, string b) => object.ReferenceEquals(a, b);
    internal static string Kind(object o) {
        if (o is string s && !(s.Length == 0)) { return s; }
        if (o is not int) { return "other"; }
        return o == null ? "null" : "int";
    }
    internal static int Bits(int x) => (x >>> 3) ^ ~x & 255;
    internal static Type Type() => typeof(Expressions);
    internal static int[] Numbers() => new int[] { 1, 2, 3 };
    internal static long Widen(int i) => (long) i << 2;
    internal static char Letter() => '\'';
    internal delegate int Op(int a, int b);
}
