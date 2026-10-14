using System;
using System.Collections.Generic;
namespace Org.Example.Selectorses;
public sealed class Selectors {
    internal abstract class AbstractDemandEnabledSelector : AbstractSelector {
        public abstract override bool Equals(object other);
        public abstract override int GetHashCode();
    }

    internal abstract class AbstractSelector : ISelector {
        protected internal readonly string name;
        protected internal readonly List<string> seen;
        protected AbstractSelector() {
            this.name = this.GetType().Name;
            this.seen = new List<string>();
        }
        public abstract bool NeverEnding { get; }
    }

    internal sealed class FromSolution : GenericSelector {
        public override bool NeverEnding { get; }
        internal FromSolution(bool random) { this.NeverEnding = random; }
        public override bool Equals(object other) => other == this;
        public override int GetHashCode() => 1;
    }
    internal abstract class GenericSelector : AbstractDemandEnabledSelector { }
    internal interface ISelector {
        bool NeverEnding { get; }
        public sealed class Lambda(Func<bool> f) : ISelector { public bool NeverEnding => f(); }
    }
}
