using Maddi.JavaCompat;
using System;
using System.Collections.Generic;
namespace Org.Example.Lang;
internal sealed class Metadata {
    private readonly IDictionary<string, object> metadata = new Dictionary<string, object>();
    internal static Metadata Of(string key, object value) => new Metadata();
    internal object Get(string key) => metadata.Get(key);
    internal abstract class Builder<T> : IBuilder where T : Builder<T> {
        internal string name;
        internal T Name(string name) {
            this.name = name;
            return (T) (object) this;
        }
    }
    internal interface IBuilder { }

    internal interface IListener<T> {
        void OnEvent(T @event);
        public sealed class Lambda(Action<T> f) : IListener<T> { public void OnEvent(T @event) => f(@event); }
    }

    internal sealed class Ok : IResult {
        public bool Success => true;
        internal bool Check() => ((IResult) this).Failed();
    }

    internal interface IResult {
        bool Success { get; }
        bool Failed() => !Success;
        public sealed class Lambda(Func<bool> f) : IResult { public bool Success => f(); }
    }

    internal interface IStartListener : IListener<string> {
        public sealed class Lambda(Action<string> f) : IStartListener { public void OnEvent(string @event) => f(@event); }
    }
}
