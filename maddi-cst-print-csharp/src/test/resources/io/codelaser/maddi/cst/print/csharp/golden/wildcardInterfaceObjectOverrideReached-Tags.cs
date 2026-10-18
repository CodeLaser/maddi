namespace Org.Example.Tagses;
public sealed class Tags {
    public sealed class Tag<T> : ITag {
        public override string ToString() => "t";
        public T First() => default;
        public int Size() => 0;
        object ITag.First() => First();
        int ITag.Size() => Size();
    }
    public interface ITag {
        object First();
        int Size();
    }

    internal sealed class Uses {
        private ITag tag = new Tag<string>();
        internal int C() => tag.Size() + tag.ToString().Length;
    }
}
