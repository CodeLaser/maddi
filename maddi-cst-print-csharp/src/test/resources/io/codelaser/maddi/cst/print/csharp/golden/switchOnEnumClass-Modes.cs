using System;
namespace Org.Example.Modeses;
public sealed class Modes {
    internal static bool Asserted(Mode mode) => mode switch { { Name: "FULL_ASSERT" } => true, { Name: "FAST" } => false };
    internal static int Weight(Mode mode) {
        int w = 0;
        switch (mode) {
            case { Name: "FAST" }:
                w++;
                goto case_FULL_ASSERT;

            case { Name: "FULL_ASSERT" }:
                case_FULL_ASSERT: ;
                w += mode.Level();
                break;
        }
        return w;
    }

    internal sealed class Mode : IComparable<Mode> {
        public static readonly Mode Fast = new Mode(1) { Name = "FAST", Ordinal = 0 };
        public static readonly Mode FullAssert = new Mode(3) { Name = "FULL_ASSERT", Ordinal = 1 };
        public string Name { get; private init; }
        public int Ordinal { get; private init; }
        public int CompareTo(Mode other) => Ordinal.CompareTo(other.Ordinal);
        public override string ToString() => Name;
        public static Mode[] Values() => new Mode[] { Fast, FullAssert };
        public static Mode ValueOf(string name) => Array.Find(Values(), v => v.Name == name) ?? throw new ArgumentException(name);
        private readonly int level;
        private Mode(int level) { this.level = level; }
        internal int Level() => level;
    }
}
