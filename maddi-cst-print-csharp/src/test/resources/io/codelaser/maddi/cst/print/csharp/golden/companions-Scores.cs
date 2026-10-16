using System;
using System.Collections.Generic;
namespace Org.Example.Scoreses;
public sealed class Scores {
    internal static int Compare(IScore a, IScore b) => ((IScore) a).CompareTo(b);
    internal static bool AnyZero(List<IScore> scores) {
        foreach (IScore score in scores) { if (score.IsZero() || score.Negate().IsZero()) { return true; } }
        return false;
    }
    internal static string Describe<S>(IDirector<S> director, S solution) => director.Calculate(solution).ToShortString();
    internal static int CompareInner(IInner a, IInner b) => ((IInner) a).CompareTo(b);
    public interface IDirector<Solution_, Score_> : IDirector<Solution_> where Score_ : IScore<Score_> {
        Score_ Calculate(Solution_ solution);
        List<Score_> History();
        IScore IDirector<Solution_>.Calculate(Solution_ solution) => Calculate(solution);
    }
    public interface IDirector<Solution_> { IScore Calculate(Solution_ solution); }

    public sealed record Inner<Score_>(Score_ Raw, int Unassigned)
        : IComparable<Inner<Score_>>, IInner where Score_ : IScore<Score_> {
        public int CompareTo(Inner<Score_> other) => Raw.CompareTo(other.Raw);
        IScore IInner.Raw => Raw;
        int IInner.Unassigned => Unassigned;
        int IComparable.CompareTo(object other) => CompareTo((Inner<Score_>) other!);
    }
    public interface IInner : IComparable {
        IScore Raw { get; }
        int Unassigned { get; }
    }

    internal sealed class MinCollector<A, P> : IMinCollector where P : IComparable<P> {
        private readonly Func<A, P> mapper;
        internal MinCollector(Func<A, P> mapper) { this.mapper = mapper; }
        public override bool Equals(object o) {
            if (o == null || this.GetType() != o.GetType()) { return false; }
            MinCollector<A, P> that = (MinCollector<A, P>) o;
            return object.Equals(mapper, that.mapper);
        }
        public override int GetHashCode() => mapper.GetHashCode();
    }
    internal interface IMinCollector { }

    public interface IScore<Score_> : IComparable<Score_>, IScore where Score_ : IScore<Score_> {
        Score_ Add(Score_ other);
        Score_ Negate();
        bool IsZero();
        string ToShortString();
        IScore IScore.Negate() => Negate();
        bool IScore.IsZero() => IsZero();
        string IScore.ToShortString() => ToShortString();
        int IComparable.CompareTo(object other) => CompareTo((Score_) other!);
    }
    public interface IScore : IComparable {
        IScore Negate();
        bool IsZero();
        string ToShortString();
    }

    public sealed record SimpleScore(int Value) : IScore<SimpleScore> {
        public SimpleScore Add(SimpleScore other) => new SimpleScore(Value + other.Value);
        public SimpleScore Negate() => new SimpleScore(-Value);
        public bool IsZero() => Value == 0;
        public string ToShortString() => "" + Value;
        public int CompareTo(SimpleScore other) => Value.CompareTo(other.Value);
    }
}
