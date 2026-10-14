using System;
namespace Org.Example.Stores;
public sealed class Store {
    private readonly object[] values = new object[4];
    internal V Get<V>(int i) => (V) values[i];
    internal string Name() {
        string s = Get<string>(0);
        return s ?? ((Func<string>) (() => {
            string fallback = "x";
            return fallback + values.Length;
        }))();
    }
}
