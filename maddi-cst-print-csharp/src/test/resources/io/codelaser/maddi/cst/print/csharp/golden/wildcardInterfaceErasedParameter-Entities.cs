namespace A;
internal sealed class Entities {
    private readonly IDescriptor descriptor;
    internal Entities(IDescriptor descriptor) { this.descriptor = descriptor; }
    internal bool Check(object e) => descriptor.Movable(default, e) && !(descriptor.Label().Length == 0);
    internal sealed class Descriptor<S> : IDescriptor {
        internal bool Movable(S solution, object entity) => solution != null || entity != null;
        internal string Label() => "d";
        bool IDescriptor.Movable(object solution, object entity) => Movable((S) solution, entity);
        string IDescriptor.Label() => Label();
    }
    internal interface IDescriptor {
        internal bool Movable(object solution, object entity);
        internal string Label();
    }
}
