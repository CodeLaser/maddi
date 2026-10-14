using Maddi.JavaCompat;
using System.Collections.Generic;
namespace Org.Example.Conv;
internal sealed class Factory<E> { internal int Size() => 3; }
internal static class Factory {
    internal static T None<T>() => default;
    internal static string Escape(char c) {
        switch (c) {
            case '\b': return "\b";
            default: return "";
        }
    }
    internal static int OrZero(int? i, int? count) => i == null ? 0 : (int) i + (int) count;
    internal static int Index(IDictionary<string, int> map, string key, int? boxed) {
        IDictionary<int, string> names = new Dictionary<int, string>();
        string name = names.Get((int) boxed);
        int i = (int) map.GetValueOrNull(key);
        if ((bool) ((object) true)) { return (int) boxed; }
        return i + name.Length;
    }
}
internal sealed class Item<E> {
    internal readonly ISet<E>[] buckets = new ISet<E>[2];
    internal int N(Factory<E> f) => f.Size();
}
