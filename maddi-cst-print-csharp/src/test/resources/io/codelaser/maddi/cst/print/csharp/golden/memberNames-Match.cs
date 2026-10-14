namespace Org.Example.Members;
internal sealed class Match<TEmbedded> {
    internal TEmbedded Embedded { get; }
    internal Match(TEmbedded embedded) { this.Embedded = embedded; }
}
internal sealed class Outcome {
    private readonly Result result;
    internal Outcome(Result result) { this.result = result; }
    internal Result ResultValue() => result;
    internal enum Result { Success, Failure }
}
