using System;
using System.Reflection;
namespace Org.Example.Attr;
internal sealed class Tools {
    internal static bool Ranked(ToolAttribute tool) => tool.Ranker != typeof(Tools.ToolAttribute.INullRanker);
    [Tool("Adds", Name = "add")]
    internal int Add(int a, int b) => a + b;

    [Obsolete]
    internal int Old() => 0;
    internal static string NameOf(ToolAttribute tool) => tool.Name;
    [AttributeUsage(AttributeTargets.Method)]
    internal sealed class ToolAttribute : Attribute {
        public ToolAttribute() { }
        public ToolAttribute(params string[] value) { Value = value; }
        public string Name { get; set; } = "";
        public string[] Value { get; set; } = new string[] { "" };
        public bool Required { get; set; } = true;
        public Type Ranker { get; set; } = typeof(Tools.ToolAttribute.INullRanker);
        public interface INullRanker : Tools.ToolAttribute.IRanker {
            public sealed class Lambda(Func<MethodInfo, int> f) : INullRanker { public int Rank(MethodInfo m) => f(m); }
        }

        public interface IRanker {
            int Rank(MethodInfo m);
            public sealed class Lambda(Func<MethodInfo, int> f) : IRanker { public int Rank(MethodInfo m) => f(m); }
        }
    }
}
