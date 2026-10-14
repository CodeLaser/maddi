using System;
namespace Org.Example.Attr;
internal sealed class Tools {
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
    }
}
