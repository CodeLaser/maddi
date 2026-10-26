namespace A;
internal sealed class Kinds {
    internal static string Statement(object o) {
        switch (o) {
            case string or int: return "known";
            default: break;
        }
        return "?";
    }
    internal static string Expression(object o) => o switch { string or int => "known", _ => "?" };
}
