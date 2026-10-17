using Maddi.JavaCompat;
using System;
using System.Collections.Generic;
namespace Org.Example.Eventses;
public sealed class Events {
    internal interface IPhaseListener {
        void PhaseStarted();
        public sealed class Lambda(Action f) : IPhaseListener { public void PhaseStarted() => f(); }
    }

    internal sealed class PhaseSupport : Support<IPhaseListener> {
        internal void Fire() { listeners.ForEach((IPhaseListener p0) => p0.PhaseStarted()); }
    }

    internal abstract class Support<E> {
        protected internal readonly List<E> listeners = new List<E>();
        internal void Add(E listener) { listeners.Add(listener); }
    }
}
