using System;
namespace A;
internal sealed class Terminations {
    internal sealed record Inner<Score_>(Score_ Raw, int Unassigned) : IInner where Score_ : IScore<Score_> {
        IScore IInner.Raw => Raw;
        int IInner.Unassigned => Unassigned;
        TOpen_ IInner.Open_<TOpen_>(IInnerOpener_<TOpen_> o) => o.With<Score_>(this);
    }
    internal interface IInner {
        IScore Raw { get; }
        int Unassigned { get; }
        TOpen_ Open_<TOpen_>(IInnerOpener_<TOpen_> o);
    }
    internal interface IInnerOpener_<out TOpen_> { TOpen_ With<Score_>(Inner<Score_> x) where Score_ : IScore<Score_>; }
    internal static class Inner {
        internal static Inner<Score_> Of<Score_>(Score_ score) where Score_ : IScore<Score_> => new Inner<Score_>(
            score,
            0);
    }

    internal sealed class Scope<Solution_> {
        internal IInner Last { get; private set; }
        internal IInner Best() => (IInner) (object) Last;
        internal void SetBest<Score_>(Inner<Score_> score) where Score_ : IScore<Score_> { Last = score; }
    }

    internal interface IScore<Score_> : IComparable<Score_>, IScore where Score_ : IScore<Score_> {
        Score_ Add(Score_ other);
        bool Feasible();
        bool IScore.Feasible() => Feasible();
        TOpen_ IScore.Open_<TOpen_>(IScoreOpener_<TOpen_> o) => o.With<Score_>(this);
        int IComparable.CompareTo(object other) => CompareTo((Score_) other!);
    }
    internal interface IScore : IComparable {
        bool Feasible();
        TOpen_ Open_<TOpen_>(IScoreOpener_<TOpen_> o);
    }
    internal interface IScoreOpener_<out TOpen_> { TOpen_ With<Score_>(IScore<Score_> x) where Score_ : IScore<Score_>; }

    internal sealed class Termination<Solution_> {
        private readonly IScore limit;
        internal Termination(IScore limit) { this.limit = limit; }
        internal bool Terminated(Scope<Solution_> scope) => new IsTerminatedOpener_(this, true, scope.Best().Raw).Call();
        internal void Reset(Scope<Solution_> scope, IScore score) {
            new SetBestOpener_(scope, new OfOpener_((IScore) score).Call()).Call();
        }
        internal void Again(Scope<Solution_> scope) { new SetBestOpener2_(scope, scope.Last).Call(); }
        internal double Gradient(Scope<Solution_> scope) => new GradientOpener_(this, scope.Best(), scope.Best().Raw)
            .Call();
        internal double Gradient<Score_>(Inner<Score_> start, Score_ score) where Score_ : IScore<Score_> => 0.5;
        internal void Check(Scope<Solution_> scope) {
            var best = scope.Best();
            new AssertScoreOpener_(this, best, "check").Call();
        }
        private void AssertScore<Score_>(Inner<Score_> score, object what) where Score_ : IScore<Score_> { }
        internal void Predict<Score_>(Scope<Solution_> scope, Score_ hint) where Score_ : IScore<Score_> {
            this.AssertScore<Score_>((Inner<Score_>) (object) scope.Best(), "predict");
        }
        internal IScore Doubled() => new TwiceOpener_(this, (IScore) limit).Call();
        private bool IsTerminated<Score_>(bool initialized, Score_ score) where Score_ : IScore<Score_> =>
            initialized &&
            score.Feasible();
        private Score_ Twice<Score_>(Score_ score) where Score_ : IScore<Score_> => score.Add(score);
        private sealed class IsTerminatedOpener_(Termination<Solution_> target_, bool initialized, IScore score) : IScoreOpener_<bool> {
            internal bool Call() => score.Open_(this);
            bool IScoreOpener_<bool>.With<Score_>(IScore<Score_> x) => target_.IsTerminated(initialized, (Score_) x);
        }

        private sealed class SetBestOpener_(Scope<Solution_> target_, IInner score) : IInnerOpener_<object> {
            internal object Call() => score.Open_(this);
            object IInnerOpener_<object>.With<Score_>(Inner<Score_> x) { target_.SetBest(x); return this; }
        }

        private sealed class OfOpener_(IScore score) : IScoreOpener_<IInner> {
            internal IInner Call() => score.Open_(this);
            IInner IScoreOpener_<IInner>.With<Score_>(IScore<Score_> x) => Inner.Of((Score_) x);
        }

        private sealed class SetBestOpener2_(Scope<Solution_> target_, IInner score) : IInnerOpener_<object> {
            internal object Call() => score.Open_(this);
            object IInnerOpener_<object>.With<Score_>(Inner<Score_> x) { target_.SetBest(x); return this; }
        }

        private sealed class GradientOpener_(Termination<Solution_> target_, IInner start, IScore score) : IInnerOpener_<double> {
            internal double Call() => start.Open_(this);
            double IInnerOpener_<double>.With<Score_>(Inner<Score_> x) => target_.Gradient(x, (Score_) score);
        }

        private sealed class AssertScoreOpener_(Termination<Solution_> target_, IInner score, string what) : IInnerOpener_<object> {
            internal object Call() => score.Open_(this);
            object IInnerOpener_<object>.With<Score_>(Inner<Score_> x) { target_.AssertScore(x, what); return this; }
        }

        private sealed class TwiceOpener_(Termination<Solution_> target_, IScore score) : IScoreOpener_<IScore> {
            internal IScore Call() => score.Open_(this);
            IScore IScoreOpener_<IScore>.With<Score_>(IScore<Score_> x) => target_.Twice((Score_) x);
        }
    }
}
