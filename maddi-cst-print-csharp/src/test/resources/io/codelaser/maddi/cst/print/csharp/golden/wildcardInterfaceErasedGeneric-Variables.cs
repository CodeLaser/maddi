using System.Collections.Generic;
namespace A;
internal sealed class Variables {
    private readonly IVariable variable;
    internal Variables(IVariable variable) { this.variable = variable; }
    internal object Read(object e) => variable.Value(e);
    internal sealed class ListVariable<S> : Variable<S> {
        internal override List<object> Value(object entity) => new List<object> { entity };
    }

    internal class Variable<S> : IVariable {
        internal virtual object Value(object entity) => (object) entity;
        internal S Solution() => default;
        object IVariable.Value(object entity) => Value(entity);
        object IVariable.Solution() => Solution();
    }
    internal interface IVariable {
        internal object Value(object entity);
        internal object Solution();
    }
}
