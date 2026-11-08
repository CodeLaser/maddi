using Maddi.JavaCompat;
using System.Collections.Generic;
namespace A;
internal sealed class Graphs {
    private readonly IDictionary<IVariable<object, object>, string> names =
        new Dictionary<IVariable<object, object>, string>();
    private readonly IDictionary<string, Descriptor> all = new Dictionary<string, Descriptor>();
    private readonly IDictionary<string, Genuine> genuine = new Dictionary<string, Genuine>();
    internal string Lookup<S, E>(IVariable<S, E> v) => names.Get(v);
    internal string LookupOr<S, E>(IVariable<S, E> v) => names.GetOrDefault(v, "none");
    internal object First<TS, TE>(IVariable<TS, TE> v) => v.Entity(null);
    internal void Merge() { all.PutAll(genuine); }
    internal void Insert<T>(List<object> into, List<T> items) { into.InsertAll(0, items); }
    internal class Descriptor { }
    internal sealed class Genuine : Descriptor { }
    internal interface IVariable<S, out E> {
        string Name();
        E Entity(List<S> s);
    }
}
