using System;
namespace Org.Example.Mappingses;
public sealed class Mappings {
    internal abstract class Stream {
        protected internal abstract object DefaultMapping();
        internal Func<string, int> Lengths() => (Func<string, int>) DefaultMapping();
    }

    internal sealed class UniStream : Stream {
        protected internal override Func<string, int> DefaultMapping() => (string p0) => p0.Length;
    }
}
