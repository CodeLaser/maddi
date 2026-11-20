using System;
using System.Collections.Generic;
namespace A;
internal sealed class Providers<S> {
    private readonly List<IGenuine<S>> entities = new List<IGenuine<S>>();
    internal List<ISwap<S>> Build() {
        List<ISwap<S>> result = new List<ISwap<S>>();
        foreach (var entity in entities) {
            if (entity.Basic()) { result.Add(entity.Open_(new ProvidersSwapOpener_<S>())); }
        }
        return result;
    }
}
internal sealed class ProvidersSwapOpener_<S> : IGenuineOpener_<S, ISwap<S>> {
    ISwap<S> IGenuineOpener_<S, ISwap<S>>.With<E>(IGenuine<S, E> entity) => new Swap<S, E>(entity);
}
internal interface IGenuine<S, E> : IPlanning<S, E>, IGenuine<S> {
    bool Basic();
    bool IGenuine<S>.Basic() => Basic();
    TOpen_ IGenuine<S>.Open_<TOpen_>(IGenuineOpener_<S, TOpen_> o) => o.With<E>(this);
}
internal interface IGenuine<S> {
    bool Basic();
    TOpen_ Open_<TOpen_>(IGenuineOpener_<S, TOpen_> o);
}
internal interface IGenuineOpener_<S, out TOpen_> { TOpen_ With<E>(IGenuine<S, E> x); }
internal interface IPlanning<S, E> { Type Type(); }
internal sealed class PlanningLambda<S, E>(Func<Type> f) : IPlanning<S, E> { public Type Type() => f(); }
internal sealed class Swap<S, E> : ISwap<S> {
    private readonly IPlanning<S, E> entity;
    internal Swap(IPlanning<S, E> entity) { this.entity = entity; }
    internal string Name() => entity.Type().Name;
    string ISwap<S>.Name() => Name();
}
internal interface ISwap<S> { internal string Name(); }
