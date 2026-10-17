namespace Org.Example.Shapes;
public sealed class Counter {
    public int Count { get; private set; }
    public string Name { get; }
    public const int LIMIT = 10;
    public Counter(string name) { this.Name = name; }
    public void Increment() {
        if (Count < LIMIT) { Count++; } else if (Count == LIMIT) { Count = 0; } else { Count += 2; }
    }
    public override string ToString() => Name + ":" + Count;
}
