using System;
namespace A;
internal sealed class Factories {
    internal interface IFactory {
        Factories.IFactory.ITemplate Create(Factories.IFactory.IInput @in);
        Factories.IFactory.ITemplate Twice(Factories.IFactory.IInput @in) {
            Create(@in);
            return Create(@in);
        }
        interface IInput { }
        interface ITemplate { }
    }
    internal sealed class FactoryLambda(Func<Factories.IFactory.IInput, Factories.IFactory.ITemplate> f) : IFactory { public Factories.IFactory.ITemplate Create(Factories.IFactory.IInput @in) => f(@in); }
    internal sealed class Plain : IFactory { public IFactory.ITemplate Create(IFactory.IInput @in) => null; }
    internal sealed class Use { internal static IFactory Make(IFactory.ITemplate t) => new FactoryLambda(@in => t); }
}
