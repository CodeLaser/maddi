using System;
namespace Org.Example.Phasescopes;
public sealed class PhaseScopes {
    internal static bool Zero<Solution_>(PhaseScope<Solution_> scope) => scope.GetBest().Raw.IsZero();
    internal static string Log<Solution_>(PhaseScope<Solution_> scope) => "" + scope.GetBest();
    internal static Score_ Calculate<Solution_, Score_>(PhaseScope<Solution_> scope, Solution_ s)
        where Score_ : IScore<Score_> {
        IDirector<Solution_, Score_> d = (IDirector<Solution_, Score_>) (object) scope.GetDirector();
        return d.Calculate(s);
    }
    internal static T NewInstance<T>(object bean, string name, Type clazz) => default;
    internal static IAcceptor Acceptor(Type c) => NewInstance<IAcceptor>(null, "a", c);
    internal static object Any(Type c) => NewInstance<object>(null, "a", c);
    internal static SimpleScore Simple(PhaseScope<string> scope) {
        Inner<SimpleScore> best = (Inner<SimpleScore>) (object) scope.GetBest();
        return best.Raw;
    }
    internal interface IAcceptor { }
    public interface IDirector<Solution_, Score_> : IDirector<Solution_> where Score_ : IScore<Score_> {
        Score_ Calculate(Solution_ solution);
        void Reset();
        IScore IDirector<Solution_>.Calculate(Solution_ solution) => Calculate(solution);
        void IDirector<Solution_>.Reset() => Reset();
    }
    public interface IDirector<Solution_> {
        IScore Calculate(Solution_ solution);
        void Reset();
    }

    public sealed record Inner<Score_>(Score_ Raw) : IInner where Score_ : IScore<Score_> {
        internal bool Zero() => Raw.IsZero();
        IScore IInner.Raw => Raw;
        bool IInner.Zero() => Zero();
    }
    public interface IInner {
        IScore Raw { get; }
        internal bool Zero();
    }

    internal sealed class PhaseScope<Solution_> {
        internal SolverScope<Solution_> solverScope;
        public IDirector<Solution_> GetDirector() => solverScope.GetDirector();
        public IInner GetBest() => solverScope.GetBest();
    }

    public interface IScore<Score_> : IScore where Score_ : IScore<Score_> {
        bool IsZero();
        public sealed class Lambda(Func<bool> f) : IScore<Score_> { public bool IsZero() => f(); }
        bool IScore.IsZero() => IsZero();
    }
    public interface IScore { bool IsZero(); }
    public sealed record SimpleScore(int Value) : IScore<SimpleScore> { public bool IsZero() => Value == 0; }
    internal sealed class SolverScope<Solution_> {
        private IInner best;
        private IDirector<Solution_> director;
        public IInner GetBest() => (IInner) (object) best;
        public IDirector<Solution_> GetDirector() => (IDirector<Solution_>) director;
    }
}
