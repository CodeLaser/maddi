using System;
namespace Org.Example.Stores;
public sealed class Store {
    private readonly object[] values = new object[4];
    internal V Get<V>(int i) => (V) values[i];
    internal static string Justify<A>(Func<A, IScore, string> f, A a) => f(a, default);
    internal string Justified() => Justify(Get<Func<string, IScore, string>>(1), "a");
    internal string Name() {
        string s = Get<string>(0);
        return s ?? ((Func<string>) (() => {
            string fallback = "x";
            return fallback + values.Length;
        }))();
    }

    internal interface IScore<Score_> : IScore where Score_ : IScore<Score_> {
        bool Zero();
        int Level();
        bool IScore.Zero() => Zero();
        int IScore.Level() => Level();
    }
    internal interface IScore {
        bool Zero();
        int Level();
    }
}
