namespace Org.Example.Geo;
internal sealed record Point(int X, int Y) {
    internal Point Scale(int f) => new Point(X * f, Y * f);
    internal static Point Origin() => new Point(0, 0);
    public bool Equals(Point o) => o is Point { X: var px, Y: var py } && px == X && py == Y;
    public override int GetHashCode() => 31 * X + Y;
}
internal sealed record Line(Point From, Point To) {
    internal static int Dx(object o) => o switch {
        Line { From: Point { X: var x1, Y: var y1 }, To: Point { X: var x2, Y: var y2 } } => x2 - x1,
        _ => 0
    };

    internal static bool Vertical(object o) {
        if (o is Line { From: Point { X: var x1, Y: var y1 }, To: var to } && to.X == x1) { return true; }
        return false;
    }
}
