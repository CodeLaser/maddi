using System;
using System.Collections.Generic;
namespace Org.Example.Tokens;
internal sealed class Json {
    internal static T FromJson<T>(string json) {
        Console.Out.WriteLine(typeof(T).FullName);
        return default;
    }
    internal static List<T> All<T>(string json) => new List<T> { FromJson<T>(json) };
    internal static string Describe<T>(Type type) => type.FullName;
    internal static string Use(Type unknown) => Describe<object>(unknown);
    internal static int? Number(string json) => FromJson<int>(json);
}
