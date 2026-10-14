namespace Org.Example.Geo;
internal sealed record Point(int X, int Y) {
    internal Point Scale(int f) => new Point(X * f, Y * f);
    internal static Point Origin() => new Point(0, 0);
}
