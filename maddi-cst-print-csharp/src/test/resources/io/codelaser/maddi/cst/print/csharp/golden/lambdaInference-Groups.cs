using System.Collections.Generic;
namespace Org.Example.Inference;
internal sealed class Groups {
    internal static IConstructor<T> GroupBy<K, T>(K key, Builder<K, T> builder) => null;
    internal static IConstructor<List<string>> ByReference(string key) => GroupBy<string, List<string>>(
        key,
        (p0, p1, p2) => new Node<string, List<string>>(p0, p1, p2));

    internal static IConstructor<List<string>> ByLambda(string key) => GroupBy<string, List<string>>(
        key,
        (k, i, next) => new Node<string, List<string>>(k, i, next));
    internal delegate object Builder<K, T>(K key, int index, ILifecycle<T> next);
    internal interface IConstructor<T> { }
    internal interface ILifecycle<T> { }
    internal sealed class Node<K, T> { internal Node(K key, int index, ILifecycle<T> next) { } }
}
