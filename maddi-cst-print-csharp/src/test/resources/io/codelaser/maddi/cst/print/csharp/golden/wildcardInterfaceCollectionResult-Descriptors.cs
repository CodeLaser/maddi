using System.Collections.Generic;
namespace A;
internal sealed class Descriptors {
    private readonly IEntity entity;
    private readonly IShadow shadow;
    internal Descriptors(IEntity entity, IShadow shadow) {
        this.entity = entity;
        this.shadow = shadow;
    }

    internal int Count() {
        int n = entity.Shadows.Count;
        foreach (var s in entity.All()) { n += s.Name().Length; }
        return n + shadow.Entity.Shadows[0].Name().Length;
    }

    internal sealed class Entity<S> : IEntity {
        internal List<Shadow<S>> Shadows { get; } = new List<Shadow<S>>();
        internal ICollection<Shadow<S>> All() => Shadows;
        IReadOnlyList<IShadow> IEntity.Shadows => Shadows;
        IEnumerable<IShadow> IEntity.All() => All();
    }
    internal interface IEntity {
        internal IReadOnlyList<IShadow> Shadows { get; }
        internal IEnumerable<IShadow> All();
    }

    internal sealed class Shadow<S> : IShadow {
        internal Entity<S> Entity { get; }
        internal Shadow(Entity<S> entity) { this.Entity = entity; }
        internal string Name() => "s";
        IEntity IShadow.Entity => Entity;
        string IShadow.Name() => Name();
    }
    internal interface IShadow {
        internal IEntity Entity { get; }
        internal string Name();
    }
}
