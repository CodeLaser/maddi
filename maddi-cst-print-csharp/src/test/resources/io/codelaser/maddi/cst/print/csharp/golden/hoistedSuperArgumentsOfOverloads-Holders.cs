using System.Collections.Generic;
namespace Org.Example.Overloads;
public sealed class Holders {
    internal static Holder<string> Empty(List<string> names) => new HolderImpl(default, names);
    internal abstract class Holder<T> {
        internal readonly T value;
        internal Holder(T value, List<T> more) { this.value = value; }
        internal Holder(int count, List<T> more) { this.value = more[count]; }
        internal abstract T Describe();
    }

    private sealed class HolderImpl : Holder<string> {
        internal HolderImpl(string p0, List<string> p1) : base(p0, p1) { }
        internal override string Describe() => "none";
    }
}
