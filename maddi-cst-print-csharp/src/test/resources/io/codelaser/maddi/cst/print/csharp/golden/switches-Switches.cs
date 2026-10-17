using System;
namespace Org.Example.Flow;
internal sealed class Switches {
    internal static int FallThrough(int k) {
        int r = 0;
        switch (k) {
            case 1:
                r += 1;
                goto case 2;

            case 2:
                r += 2;
                break;

            case 3:
                r = 3;
                goto default;

            default:
                r--;
                break;
        }
        return r;
    }

    internal static string Arrows(int k) {
        switch (k) {
            case 1: case 2: return "small";
            case 3:
                Console.Out.WriteLine("three");
                break;
            default: break;
        }
        return "large";
    }

    internal static int Expression(string s) => s switch {
        "a" or "b" => 1,
        "c" => new Func<int>(() => {
            int x = 2;
            return x * 2;
        })(),
        _ => throw new ArgumentException(s)
    };
}
