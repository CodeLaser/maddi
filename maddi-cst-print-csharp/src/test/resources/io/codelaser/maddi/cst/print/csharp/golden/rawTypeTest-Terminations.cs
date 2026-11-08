using System;
namespace A;
internal sealed class Terminations<S> {
    private readonly ISolverTermination<S> termination;
    internal Terminations(ISolverTermination<S> termination) { this.termination = termination; }
    internal bool Done(S s) {
        if (termination is IPhaseTermination<S> phase) { return phase.PhaseDone(s); }
        return termination is IPhaseTermination<S> && termination.SolverDone(s);
    }
}
internal interface IPhaseTermination<in S> : ISolverTermination<S> { bool PhaseDone(S s); }
internal interface ISolverTermination<in S> { bool SolverDone(S s); }
internal sealed class SolverTerminationLambda<S>(Func<S, bool> f) : ISolverTermination<S> { public bool SolverDone(S s) => f(s); }
