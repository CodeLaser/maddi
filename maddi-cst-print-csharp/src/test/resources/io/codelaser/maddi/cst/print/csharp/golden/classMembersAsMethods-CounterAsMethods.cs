namespace Org.Example.Shapes;
public sealed class Counter {
    private int count;
    private readonly string name;
    public const int LIMIT = 10;
    public Counter(string name) { this.name = name; }
    public int GetCount() => count;
    public void Increment() {
        if (count < LIMIT) { count++; } else if (count == LIMIT) { count = 0; } else { count += 2; }
    }
    public string Name() => name;
    public override string ToString() => name + ":" + count;
}
