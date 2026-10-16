using System;
using System.Collections.Generic;
using System.Linq;
namespace Org.Example.Lifecycleses;
public sealed class Lifecycles {
    internal static int Counted(IBox box) => box.Count();
    internal static int Total(List<IBox> boxes) {
        int sum = 0;
        foreach (var box in boxes) { sum += Counted(box); }
        return sum;
    }
    internal static IBox Any(bool b) => b ? new Box<string>("s") : new Box<int>(1);
    internal static int CountOf(bool b) => Any(b).Count();
    internal static object First(bool b) => Any(b).Get;
    internal static int Sum(IReadOnlyList<IBox> boxes) => boxes.Count;
    public interface IActivitySupport {
        bool IsActive();
        public sealed class Lambda(Func<bool> f) : IActivitySupport { public bool IsActive() => f(); }
    }

    public class Box<T> : IBox {
        public T Get { get; }
        internal Box(T value) { this.Get = value; }
        public int Count() => 1;
        public void Set(T t) { }
        public virtual object Mapping() => default;
        object IBox.Get => Get;
        int IBox.Count() => Count();
    }
    public interface IBox {
        object Get { get; }
        int Count();
    }

    public sealed class Holder<T> {
        internal readonly object[] items = { };
        public T First() => default;
    }

    public sealed class Named : Box<string> {
        internal Named() : base("n") { }
        public override object Mapping() => "m";
    }

    public abstract class Node<Left_> where Left_ : ITuple {
        private readonly ITupleLifecycle downstream;
        protected internal Node(ITupleLifecycle downstream) { this.downstream = downstream; }
        public bool Active() => downstream.IsActive();
        public void Done() { downstream.AfterAllFactsInserted(true); }
    }
    public sealed record Pair(int Size) : ITuple { int ITuple.Size() => Size; }
    public sealed class PairNode : Node<Pair> { internal PairNode(ITupleLifecycle<Pair> next) : base(next) { } }
    public interface ITuple {
        int Size();
        public sealed class Lambda(Func<int> f) : ITuple { public int Size() => f(); }
    }

    public interface ITupleLifecycle<Tuple_> : IActivitySupport, ITupleLifecycle where Tuple_ : ITuple {
        void Insert(Tuple_ tuple);
        void AfterAllFactsInserted(bool canProduce);
        void ITupleLifecycle.AfterAllFactsInserted(bool canProduce) => AfterAllFactsInserted(canProduce);
    }
    public interface ITupleLifecycle : IActivitySupport { void AfterAllFactsInserted(bool canProduce); }

    public sealed class Typed<A> : ITyped {
        public Type Kind { get; }
        internal Typed(Type type) { this.Kind = type; }
        Type ITyped.Kind => Kind;
    }
    public interface ITyped { Type Kind { get; } }

    internal sealed class Uses {
        private Holder<object> holder;
        private ITyped typed;
        private Box<ITuple> boxed;
        internal int Items() => holder.items.Length;
        internal string Name() => typed.Kind.Name;
    }
}
