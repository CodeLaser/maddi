using System.Collections.Generic;
namespace A;
internal sealed class Models {
    internal class Base<S> {
        internal string Label() => "b";
        internal List<S> All() => null;
    }

    internal sealed class Holder {
        internal readonly IShadow shadow;
        internal Holder(IShadow shadow) { this.shadow = shadow; }
        internal string Describe() => shadow.Name + shadow.Ordinal() + shadow.Label();
    }

    internal sealed class Shadow<S, V> : Base<S>, IVariable<S, V>, IShadow {
        public string Name => "s";
        public List<V> Values(S s) => null;
        string IShadow.Name => Name;
        string IShadow.Label() => Label();
        int IShadow.Ordinal() => ((IVariable<S, V>) this).Ordinal();
    }
    internal interface IShadow {
        string Name { get; }
        internal string Label();
        internal int Ordinal();
    }

    internal interface IVariable<in S, V> {
        string Name { get; }
        List<V> Values(S s);
        int Ordinal() => 0;
    }
}
