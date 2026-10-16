using System;
namespace Org.Example.Color;
public sealed class Palette {
    internal static string Name(Primary? p) {
        switch (p) {
            case Primary.Red: return "red";
            case Primary.Green: case Primary.Blue: return "other";
            default: return "?";
        }
    }
    internal static Primary? First() => Primary.Red;
    internal static bool Later(Planet a, Planet b) => a.CompareTo(b) > 0;
    internal sealed class Planet : IComparable<Planet> {
        public static readonly Planet Mercury = new Planet(3.303E23) { Name = "MERCURY", Ordinal = 0 };
        public static readonly Planet Earth = new Planet(5.976E24) { Name = "EARTH", Ordinal = 1 };
        public string Name { get; private init; }
        public int Ordinal { get; private init; }
        public int CompareTo(Planet other) => Ordinal.CompareTo(other.Ordinal);
        public override string ToString() => Name;
        public static Planet[] Values() => new Planet[] { Mercury, Earth };
        public static Planet ValueOf(string name) => Array.Find(Values(), v => v.Name == name) ?? throw new ArgumentException(name);
        private readonly double mass;
        private Planet(double mass) { this.mass = mass; }
        internal double Mass() => mass;
    }
    internal enum Primary { Red, Green, Blue }
}
