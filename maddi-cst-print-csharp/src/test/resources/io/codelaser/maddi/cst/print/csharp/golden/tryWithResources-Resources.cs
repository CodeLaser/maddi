using System;
namespace Org.Example.Io;
internal sealed class Resources {
    internal static int One() {
        using (var a = new Res())
        using (var b = new Res())
        { return a.Read() + b.Read(); }
    }

    internal static int Caught() {
        try {
            using (var r = new Res())
            { return r.Read(); }
        } catch (Exception e) when (e is InvalidOperationException || e is NotSupportedException) { return -1; } finally {
            System.gc();
        }
    }

    internal sealed class Res : IDisposable {
        public void Dispose() { }
        internal int Read() => 1;
    }
}
