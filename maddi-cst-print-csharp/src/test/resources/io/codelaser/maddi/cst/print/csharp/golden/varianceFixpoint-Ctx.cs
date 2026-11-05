using System;
using System.Collections.Generic;
namespace A;
internal interface ICtx<S> {
    S Get();
    void Exec(IMove<S> m);
}
internal interface IMove<S> {
    void Run(IView<S> v);
    void Twice(IView<S> v) {
        Run(v);
        Run(v);
    }
}
internal sealed class MoveLambda<S>(Action<IView<S>> f) : IMove<S> { public void Run(IView<S> v) => f(v); }
internal interface IView<S> {
    IMove<S> Next();
    IMove<S> Again() => Next();
}
internal sealed class ViewLambda<S>(Func<IMove<S>> f) : IView<S> { public IMove<S> Next() => f(); }
internal interface IListing<S> {
    List<IMove<S>> Moves(S s);
    int Count(S s) => Moves(s).Count;
}
internal sealed class ListingLambda<S>(Func<S, List<IMove<S>>> f) : IListing<S> { public List<IMove<S>> Moves(S s) => f(s); }
internal interface IFactory {
    ITemplate Create(IInput @in);
    ITemplate Twice(IInput @in) {
        Create(@in);
        return Create(@in);
    }
    interface IInput { }
    interface ITemplate { }
}
internal sealed class FactoryLambda(Func<IFactory.IInput, IFactory.ITemplate> f) : IFactory { public IFactory.ITemplate Create(IFactory.IInput @in) => f(@in); }
internal sealed class Use { internal static IFactory Make() => new FactoryLambda(@in => null); }
