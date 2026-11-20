using System;
using System.Collections.Generic;
namespace A;
internal sealed class Models {
    internal interface IEntity {
        List<Variable<object>> Variables { get; }
        Variable<V> Variable<V>(string name) {
            foreach (var v in Variables) { if (v().Equals(name)) { return (Variable<V>) (object) v; } }
            throw new ArgumentException(name);
        }

        Variable<V> Variable<V>(string name, Type type) {
            foreach (var v in Variables) { if (v().Equals(name)) { return (Variable<V>) (object) v; } }
            throw new ArgumentException(type.FullName);
        }
    }
    internal sealed class EntityLambda(Func<List<Variable<object>>> f) : IEntity { public List<Variable<object>> Variables => f(); }

    internal sealed class Genuine : IEntity {
        public List<Variable<object>> Variables => new List<Variable<object>>();
        internal Variable<V> GenuineValue<V>(string name) => Checked(((IEntity) this).Variable<V>(name, typeof(V)));
        internal Variable<V> Checked<V>(Variable<V> v) => v;
    }
    internal delegate string Variable<V>();
}
