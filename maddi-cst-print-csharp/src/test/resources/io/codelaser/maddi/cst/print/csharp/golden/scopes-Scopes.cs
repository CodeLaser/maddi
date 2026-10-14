namespace Org.Example.Scope;
internal sealed class Scopes {
    internal static readonly int[][] TABLE = { null, new int[] { 1, 2 }, new int[] { } };
    internal static int Later(int[] xs) {
        for (int k = 0; k < xs.Length; k++) {
            int last2 = xs[k];
            if (last2 > 0) { return last2; }
        }
        int last = -1;
        return last;
    }

    internal static string Twice(object o, object p) {
        if (o is string s) { return s; }
        if (p is string s2) { return s2 + s2; }
        return "";
    }
}
