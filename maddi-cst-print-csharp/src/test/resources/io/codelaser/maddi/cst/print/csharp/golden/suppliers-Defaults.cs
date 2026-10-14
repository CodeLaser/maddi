using System.Collections.Generic;
namespace Org.Example.Supply;
internal sealed class Defaults<R> : IDefaults where R : Defaults<R> { }
internal interface IDefaults { }
internal static class Defaults {
    internal static List<string> Names(List<string> given) => given ?? new List<string>();
    internal static List<string> Copy(List<string> given) => given ?? new List<string>();
    internal static string Name(string given) => given ?? "none";
    internal static IDefaults Raw() => null;
}
