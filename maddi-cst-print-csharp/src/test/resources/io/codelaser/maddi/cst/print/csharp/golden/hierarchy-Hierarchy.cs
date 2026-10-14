using System;
namespace Org.Example.Shapes;
internal interface IShape {
    double Area { get; }
    string Describe => "shape";
    public sealed class Lambda(Func<double> f) : IShape { public double Area => f(); }
}
internal abstract class Base : IShape {
    protected internal readonly double scale;
    protected internal Base(double scale) { this.scale = scale; }
    public abstract double Unit { get; }
    public double Area => Unit * scale;
}
internal sealed class Square : Base {
    private readonly double side;
    internal Square(double side) : base(1.0) { this.side = side; }
    public override double Unit => side * side;
    public string Describe => "square";
}
