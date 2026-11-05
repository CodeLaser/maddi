using Maddi.JavaCompat;
namespace A;
public class Bag<T> {
    internal Entry Add(T t) => new Entry(t);
    internal IJavaIterator<T> cursor() => null;
    internal sealed class Cursor {
        internal Bag<T>.Cursor.Step current;
        internal sealed class Step { }
    }

    internal sealed class Entry {
        internal readonly T value;
        internal Entry(T value) { this.value = value; }
    }
}
internal sealed class User { internal Bag<string>.Entry First(Bag<string> bag) => bag.Add("x"); }
internal sealed class Sub<S> : Bag<S> { internal Bag<S>.Entry Again(S s) => Add(s); }
