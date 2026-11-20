using Maddi.JavaCompat;
using System;
using System.Collections.Generic;
using System.Text;
namespace A;
internal sealed class MetaModels {
    internal sealed class Entity<S, E> {
        private readonly List<IShadow<S, E>> variables = new List<IShadow<S, E>>();
        internal void Add(IShadow<S, E> v) { variables.Add(v); }
        internal string Describe() {
            var sb = new StringBuilder();
            foreach (IShadow<S, E> v in variables) {
                sb.Append(v.Name).Append(v.Type().Name).Append(JavaString.ValueOf(v.Derived));
            }
            return sb.ToString();
        }
    }

    internal sealed class Impl<S, E, V> : IShadow<S, E, V> {
        private readonly Type type;
        internal Impl(Type type) { this.type = type; }
        public Type Type() => type;
        public string Name => "n";
        public bool Derived => true;
    }

    internal interface IShadow<S, E, V> : IVariable<S, E, V>, IShadow<S, E> {
        bool Derived { get; }
        bool IShadow<S, E>.Derived => Derived;
    }
    internal interface IShadow<S, E> : IVariable<S, E> { bool Derived { get; } }

    internal interface IVariable<S, E, V> : IVariable<S, E> {
        Type Type();
        string Name { get; }
        Type IVariable<S, E>.Type() => Type();
        string IVariable<S, E>.Name => Name;
    }
    internal interface IVariable<S, E> {
        Type Type();
        string Name { get; }
    }
}
