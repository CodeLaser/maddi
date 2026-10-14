namespace Org.Example.Nodeses;
public sealed class Nodes {
    public abstract class AbstractNode<A> { internal abstract A Value(); }
    internal sealed class Hidden { }
    public sealed class TextNode : AbstractNode<string> { internal override string Value() => "x"; }
}
