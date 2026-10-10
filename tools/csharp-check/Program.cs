// The judge of maddi's Java -> C# translation: parses and compiles C# files with Roslyn and writes one line per
// diagnostic, for JavaToCSharpRatchet (maddi-run-openjdk) to read. See README.md.
//
//   csharp-check <argument file>
//
// The argument file has one C# file per line. Output, tab-separated, one diagnostic per line:
//
//   S <file> <line> <column> <code> <message>   a syntax error, from parsing the file alone
//   E <file> <line> <column> <code> <message>   an error of the compilation of all syntax-clean files together
//   F <files> <syntax-clean files>              the last line: the counts
//
// Lines and columns are 1-based. The compilation references the assemblies of the running .NET, so the translated
// code is judged against the BCL as it is.

using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;

if (args.Length != 1)
{
    Console.Error.WriteLine("usage: csharp-check <argument file>");
    return 2;
}

var files = File.ReadAllLines(args[0]).Select(l => l.Trim()).Where(l => l.Length > 0).ToList();
var parseOptions = new CSharpParseOptions(LanguageVersion.Latest);
var clean = new List<SyntaxTree>();
foreach (var file in files)
{
    var tree = CSharpSyntaxTree.ParseText(File.ReadAllText(file), parseOptions, path: file);
    var errors = tree.GetDiagnostics().Where(d => d.Severity == DiagnosticSeverity.Error).ToList();
    errors.ForEach(d => Write("S", d));
    if (errors.Count == 0) clean.Add(tree);
}

var references = ((string)AppContext.GetData("TRUSTED_PLATFORM_ASSEMBLIES")!)
    .Split(Path.PathSeparator)
    .Where(p => Path.GetFileName(p).StartsWith("System.") || Path.GetFileName(p) is "mscorlib.dll" or "netstandard.dll")
    .Select(p => MetadataReference.CreateFromFile(p));
var compilation = CSharpCompilation.Create("translated", clean, references,
    new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary, nullableContextOptions: NullableContextOptions.Disable));
foreach (var d in compilation.GetDiagnostics().Where(d => d.Severity == DiagnosticSeverity.Error))
{
    Write("E", d);
}
Console.WriteLine($"F\t{files.Count}\t{clean.Count}");
return 0;

static void Write(string kind, Diagnostic d)
{
    var span = d.Location.GetLineSpan();
    var message = d.GetMessage().Replace('\t', ' ').Replace('\n', ' ').Replace('\r', ' ');
    Console.WriteLine($"{kind}\t{span.Path}\t{span.StartLinePosition.Line + 1}\t{span.StartLinePosition.Character + 1}\t{d.Id}\t{message}");
}
