using System;
namespace A;
internal sealed class Terminations<S> {
    private readonly ISolverTermination<S> termination;
    internal Terminations(ISolverTermination<S> termination) { this.termination = termination; }
    internal bool Done(S s) {
        if (termination is IPhaseTermination<S> phase) { return phase.PhaseDone(s); }
        if (termination is IChildTermination<S> child) { return child.ChildDone(s); }
        return termination is IChildTermination<S> && termination.SolverDone(s);
    }
}
internal interface IChildTermination<S> : ISolverTermination<S> { bool ChildDone(S s); }
internal interface IPhaseTermination<S> : ITermination<S> { bool PhaseDone(S s); }
internal interface ISolverTermination<S> : ITermination<S> { bool SolverDone(S s); }
internal interface ITermination<S> { int Ordinal(); }
internal sealed class TerminationLambda<S>(Func<int> f) : ITermination<S> { public int Ordinal() => f(); }
