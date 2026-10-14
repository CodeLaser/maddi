using System.Collections.Generic;
namespace Org.Example.Hoisted;
public sealed class Holders {
    internal static Holder<string> Empty(List<string> names) => new HolderImpl(default, names);
    internal static Holder<int> One(int i) => new HolderImpl2(i, new List<int>());
    internal abstract class Holder<T> {
        internal readonly T value;
        internal Holder(T value, List<T> more) { this.value = value; }
        internal abstract T Describe();
    }

    internal sealed class Ones : Holder<int> {
        internal Ones() : base(1, new List<int>()) { }
        internal override int Describe() => value;
    }

    private sealed class HolderImpl : Holder<string> {
        internal HolderImpl(string p0, List<string> p1) : base(p0, p1) { }
        internal override string Describe() => "none";
    }

    private sealed class HolderImpl2 : Holder<int> {
        internal HolderImpl2(int p0, List<int> p1) : base(p0, p1) { }
        internal override int Describe() => value;
    }
}
