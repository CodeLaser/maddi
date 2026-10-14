namespace Org.Example.Properties;
public sealed class Person {
    public string Name { get; set; }
    internal int Age { get; }
    internal Person(int age) { this.Age = age; }
    public bool IsAdult() => Age >= 18;
}
internal sealed class Greeter {
    internal string Greet(Person p) {
        p.Name = "Ann";
        return p.Name + (p.IsAdult() ? "" : " (" + p.Age + ")");
    }
}
