using Maddi.JavaCompat;
using System;
using System.Collections.Generic;
namespace A;
internal sealed class Lookup {
    private readonly IDictionary<string, object> values = new Dictionary<string, object>();
    internal V Variable<V>(string name) => (V) values.Get(name);
    internal V Variable<V>(string name, Type type) => (V) values.Get(name);
    internal V Single<V>(string name) => (V) values.Get(name);
    internal string Use() {
        string a = Variable<string>("a", typeof(string));
        string b = Single<string>("b");
        string c = Variable<string>("c");
        return a + b + c;
    }
}
