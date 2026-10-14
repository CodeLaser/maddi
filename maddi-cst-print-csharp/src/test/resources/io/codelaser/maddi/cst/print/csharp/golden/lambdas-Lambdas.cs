using System;
using System.Text;
namespace Org.Example.Fn;
internal sealed class Lambdas {
    internal static int Len(string s) => 1;
    internal static Func<string, int> Lengths() => Lambdas.Len;
    internal static Func<int, int> Inc() => x => x + 1;
    internal static Func<StringBuilder> Fresh() => () => new StringBuilder();
    internal static Func<int, int> Block() => x => {
        int y = x * 2;
        return y + 1;
    };
}
