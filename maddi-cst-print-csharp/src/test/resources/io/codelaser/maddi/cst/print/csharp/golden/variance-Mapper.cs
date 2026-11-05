using System;
using System.Collections.Generic;
namespace A;
public delegate R Mapper<in A, out R>(A a);
internal interface ISource<out T> {
    T Next();
    IEnumerable<T> All();
    T[] Array();
    Func<int, T> At();
    Action<Action<T>> Each();
    bool Has() => Next() != null;
}
internal interface ISink<T> {
    void Add(T t);
    void AddAll(List<T> ts);
    void Give(Action<T> c);
}
internal interface IBox<T> {
    T Get();
    void Set(T t);
}
internal interface IHandler<in E> {
    void Handle(E e);
    IHandler<E> Twice() => new HandlerLambda<E>(e => {
        Handle(e);
        Handle(e);
    });
}
internal sealed class HandlerLambda<E>(Action<E> f) : IHandler<E> { public void Handle(E e) => f(e); }
internal class Animal { }
internal sealed class Dog : Animal { }
internal sealed class Use {
    internal static object First(Mapper<string, object> m, ISource<Animal> s, IHandler<Dog> h) {
        h.Handle(new Dog());
        return m;
    }

    internal static object Second(Mapper<string, string> m, ISource<Dog> s, IHandler<Animal> h) => First(
        x0_ => m(x0_),
        s,
        h);
    internal static IHandler<string> Make() => new HandlerLambda<string>((string s) => { });
}
