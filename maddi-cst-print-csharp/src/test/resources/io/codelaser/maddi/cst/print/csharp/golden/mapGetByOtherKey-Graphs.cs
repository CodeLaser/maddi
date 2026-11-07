using Maddi.JavaCompat;
using System.Collections.Generic;
namespace A;
internal sealed class Graphs {
    private readonly IDictionary<IVariable<object, object>, string> names =
        new Dictionary<IVariable<object, object>, string>();
    internal string Lookup<S, E>(IVariable<S, E> v) => names.Get(v);
    internal string LookupOr<S, E>(IVariable<S, E> v) => names.GetOrDefault(v, "none");
    internal object First<TS, TE>(IVariable<TS, TE> v) => v.Entity(default);
    internal interface IVariable<in S, out E> {
        string Name();
        E Entity(S s);
        static IVariable<object, object> Erased_(IVariable<S, E> erased) => erased as IVariable<object, object> ?? new VariableErasure_<S, E>(erased);
    }
    internal sealed class VariableErasure_<S, E> : IVariable<object, object> {
        private readonly IVariable<S, E> erased;
        internal VariableErasure_(IVariable<S, E> erased) { this.erased = erased; }
        public string Name() => erased.Name();
        public object Entity(object s) => erased.Entity((S) s);
    }
}
