namespace Org.Example.Capture;
public sealed class Steps {
    internal static IStep Make() {
        int x = 5;
        return new StepImpl(x);
    }

    internal interface IStep {
        int First();
        int Second();
    }

    private sealed class StepImpl : IStep {
        private readonly int x;
        internal StepImpl(int x) { this.x = x; }
        public int First() {
            int x = 1;
            return x;
        }
        public int Second() => x;
    }
}
