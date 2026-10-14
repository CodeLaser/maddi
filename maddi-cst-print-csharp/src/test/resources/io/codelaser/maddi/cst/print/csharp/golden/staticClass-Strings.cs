using System;
namespace Org.Example.Util;
public static class Strings {
    public const string EMPTY = "";
    public static T Max<T>(T a, T b) where T : IComparable<T> => a.CompareTo(b) >= 0 ? a : b;
}
