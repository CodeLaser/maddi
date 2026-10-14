namespace Org.Example.Clash;
public sealed class Button {
    private Listener listener;
    public Listener GetListener() => listener;
    public void SetListener(Listener listener) { this.listener = listener; }
    public delegate void Listener();
}
