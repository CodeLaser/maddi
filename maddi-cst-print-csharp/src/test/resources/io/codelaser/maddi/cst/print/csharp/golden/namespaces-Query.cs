using System;
namespace Org.Example.Exceptions.Queries;
internal sealed class Query {
    internal static void Check(string s) { if (s == null) { throw new InvalidOperationException("no " + s); } }
}
