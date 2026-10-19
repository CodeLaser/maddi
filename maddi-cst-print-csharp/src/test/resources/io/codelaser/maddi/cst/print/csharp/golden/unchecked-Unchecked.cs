using System.Collections.Generic;
namespace Org.Example.Uncheckeds;
internal sealed class Unchecked<Node_> {
    private readonly IDictionary<string, Node_> nodes = new Dictionary<string, Node_>(16);
    internal bool SeenNode(Node_[] values, Node_ v) => Unchecked.Seen((object[]) (object) values, v);
}
internal static class Unchecked {
    internal static int Weigh(IDictionary<string, int> weights) => weights.Count;
    internal static int WeighAny<W>(IDictionary<string, W> weights) => Weigh((IDictionary<string, int>) weights);
    internal static bool Seen(object[] values, object v) {
        foreach (object o in values) { if (o == v) { return true; } }
        return false;
    }
}
