using System.Numerics;
namespace Org.Example.Operatorses;
public sealed class Operators {
    internal static int CompareNegated(BigInteger x, BigInteger y) => (-x).CompareTo(y);
    internal static bool Differs(Color c, object o) => !(object.Equals(c, o));
    internal enum Color { Red, Green }
}
