using System;
namespace Org.Example.Access;
internal sealed class Outer {
    private int secret;
    internal int Read() => new Helper().used;
    internal Key<IConvertible> Raw() => null;
    internal class Base { protected internal virtual void M() { } }
    internal sealed class Derived : Base { protected internal override void M() { } }
    private sealed class Helper {
        internal int used;
        private int unused;
        internal Helper() { }
        private int Peek(Outer o) => o.secret + unused;
    }
    internal sealed class Key<T> where T : IConvertible { }
}
