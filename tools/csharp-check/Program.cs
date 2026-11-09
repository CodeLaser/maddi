// The judge of maddi's Java -> C# translation: parses and compiles C# files with Roslyn and writes one line per
// diagnostic, for JavaToCSharpRatchet (maddi-run-openjdk) to read. See README.md.
//
//   csharp-check <argument file>
//   csharp-check -serve             an argument file per line of standard input, each answered as below
//
// The argument file has one C# file per line, one line -r:<assembly> per referenced assembly beyond the BCL
// (the packages of the translation's library mappings), and the line -nullable to compile with C#'s nullable
// reference types enabled. Output, tab-separated, one diagnostic per line:
//
//   S <file> <line> <column> <code> <message>   a syntax error, from parsing the file alone
//   E <file> <line> <column> <code> <message>   an error of the compilation of all syntax-clean files together
//   N <file> <line> <column> <code> <message>   with -nullable: a nullable warning (CS86xx, CS87xx, CS88xx)
//   O <text>                                    with -run:<type>: a line the run printed (tabs as \t)
//   X <exception> <message>                     with -run:<type>: the exception the run ended with
//   F <files> <syntax-clean files>              the last line: the counts
//
// The line -run:<type> runs the static Main of the type of that simple name once the compilation has no error: the
// behavioural check of the unit tests (CommonJavaToCSharp.behaves), which compare it with the Java's run.
//
// Lines and columns are 1-based. The compilation references the assemblies of the running .NET, so the translated
// code is judged against the BCL as it is.

using System.Reflection;
using System.Runtime.Loader;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;

if (args.Length == 1 && args[0] == "-serve")
{
    // one process for many checks (maddi-cst-print-csharp's unit tests): an argument file per line of standard input,
    // each answered by its diagnostics and its F line; the platform's references are read once
    string? argFile;
    while ((argFile = Console.ReadLine()) != null)
    {
        if (argFile.Trim().Length > 0) Checker.Check(argFile.Trim());
        Console.Out.Flush();
    }
    return 0;
}
if (args.Length != 1)
{
    Console.Error.WriteLine("usage: csharp-check <argument file> | -serve");
    return 2;
}
Checker.Check(args[0]);
return 0;

/// <summary>The checks, with the references read once for the process.</summary>
static class Checker
{
    public static void Check(string argFile)
    {
        var lines = File.ReadAllLines(argFile).Select(l => l.Trim()).Where(l => l.Length > 0).ToList();
        var nullable = lines.Contains("-nullable");
        var files = lines.Where(l => !l.StartsWith("-")).ToList();
        var assemblies = lines.Where(l => l.StartsWith("-r:")).Select(l => l.Substring(3)).ToList();
        var run = lines.Where(l => l.StartsWith("-run:")).Select(l => l.Substring(5)).FirstOrDefault();
        var parseOptions = new CSharpParseOptions(LanguageVersion.Latest);
        var clean = new List<SyntaxTree>();
        foreach (var file in files)
        {
            var tree = CSharpSyntaxTree.ParseText(File.ReadAllText(file), parseOptions, path: file);
            var errors = tree.GetDiagnostics().Where(d => d.Severity == DiagnosticSeverity.Error).ToList();
            errors.ForEach(d => Write("S", d));
            if (errors.Count == 0) clean.Add(tree);
        }

        // a package's copy of a platform assembly is the platform's
        var platformNames = Platform.Keys.Select(Path.GetFileName).ToHashSet();
        var references = Platform.Values
            .Concat(assemblies.Where(p => !platformNames.Contains(Path.GetFileName(p))).Select(Reference));
        var compilation = CSharpCompilation.Create("translated", clean, references,
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary,
                nullableContextOptions: nullable ? NullableContextOptions.Enable : NullableContextOptions.Disable));
        var failed = clean.Count < files.Count;
        foreach (var d in compilation.GetDiagnostics())
        {
            if (d.Severity == DiagnosticSeverity.Error)
            {
                Write("E", d);
                failed = true;
            }
            else if (nullable && d.Severity == DiagnosticSeverity.Warning && IsNullable(d.Id)) Write("N", d);
        }
        if (run != null && !failed) Run(compilation, run, assemblies);
        Console.WriteLine($"F\t{files.Count}\t{clean.Count}");
    }

    /// <summary>
    /// Runs the static Main of the type named {@code typeName} in the compiled assembly, loaded into a collectible
    /// context with the -r: assemblies beside it, and writes what it printed (O lines) and the exception it ended
    /// with (an X line). A run of more than ten seconds ends with the exception "Timeout".
    /// </summary>
    static void Run(CSharpCompilation compilation, string typeName, List<string> assemblies)
    {
        using var image = new MemoryStream();
        var emitted = compilation.Emit(image);
        if (!emitted.Success)
        {
            foreach (var d in emitted.Diagnostics.Where(d => d.Severity == DiagnosticSeverity.Error)) Write("E", d);
            return;
        }
        image.Position = 0;
        var context = new AssemblyLoadContext("run", isCollectible: true);
        var byName = assemblies.ToDictionary(p => Path.GetFileNameWithoutExtension(p), p => p);
        context.Resolving += (c, name) => name.Name != null && byName.TryGetValue(name.Name, out var path)
            ? c.LoadFromAssemblyPath(Path.GetFullPath(path)) : null;
        var output = new StringWriter();
        var console = Console.Out;
        string? exception = null;
        try
        {
            var assembly = context.LoadFromStream(image);
            var main = assembly.GetTypes().Where(t => t.Name == typeName)
                .Select(t => t.GetMethod("Main", BindingFlags.Static | BindingFlags.Public | BindingFlags.NonPublic))
                .FirstOrDefault(m => m != null);
            if (main == null)
            {
                exception = "NoMain\tno static Main in a type " + typeName;
            }
            else
            {
                Console.SetOut(output);
                var arguments = main.GetParameters().Length == 0 ? null : new object?[] { Array.Empty<string>() };
                var task = Task.Run(() => main.Invoke(null, arguments));
                if (!task.Wait(TimeSpan.FromSeconds(10))) exception = "Timeout\tmore than ten seconds";
            }
        }
        catch (Exception e)
        {
            var inner = e is AggregateException { InnerException: not null } a ? a.InnerException : e;
            if (inner is TargetInvocationException { InnerException: not null } t) inner = t.InnerException;
            exception = inner!.GetType().Name + "\t" + inner.Message.Replace('\t', ' ').Replace('\n', ' ').Replace('\r', ' ');
        }
        finally
        {
            Console.SetOut(console);
            context.Unload();
        }
        var printed = output.ToString().Replace("\r\n", "\n");
        if (printed.EndsWith('\n')) printed = printed[..^1];
        if (printed.Length > 0 || output.ToString().Length > 0)
        {
            foreach (var line in printed.Split('\n')) Console.WriteLine("O\t" + line.Replace("\t", "\\t"));
        }
        if (exception != null) Console.WriteLine("X\t" + exception);
    }

    static readonly Dictionary<string, MetadataReference> References = new();

    /// <summary>A reference to an assembly, read once: -serve checks many files against the same ones.</summary>
    static MetadataReference Reference(string path)
    {
        if (!References.TryGetValue(path, out var reference))
        {
            reference = MetadataReference.CreateFromFile(path);
            References[path] = reference;
        }
        return reference;
    }

    /// <summary>The assemblies of the running .NET, by path: the BCL the translated code is judged against.</summary>
    static readonly Dictionary<string, MetadataReference> Platform = ((string)AppContext.GetData("TRUSTED_PLATFORM_ASSEMBLIES")!)
        .Split(Path.PathSeparator)
        .Where(p => Path.GetFileName(p).StartsWith("System.") || Path.GetFileName(p) is "mscorlib.dll" or "netstandard.dll")
        .Distinct().ToDictionary(p => p, Reference);

    // the nullable warnings: CS8597-CS8670, CS8714, CS8762-CS8777, CS8819-CS8855, ... all in these hundreds
    static bool IsNullable(string id) => id.StartsWith("CS86") || id.StartsWith("CS87") || id.StartsWith("CS88")
                                         || id == "CS8597";

    static void Write(string kind, Diagnostic d)
    {
        var span = d.Location.GetLineSpan();
        var message = d.GetMessage().Replace('\t', ' ').Replace('\n', ' ').Replace('\r', ' ');
        Console.WriteLine($"{kind}\t{span.Path}\t{span.StartLinePosition.Line + 1}\t{span.StartLinePosition.Character + 1}\t{d.Id}\t{message}");
    }
}
