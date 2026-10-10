# csharp-check

The judge of maddi's Java → C# translation (`maddi-cst-print-csharp`, #115). `JavaToCSharpRatchet` in
`maddi-run-openjdk` builds it with `dotnet build` and runs it on the translated files. The tool parses each file on
its own for syntax errors, then compiles the syntax-clean files together against the BCL of the running .NET with
Roslyn (the `Microsoft.CodeAnalysis.CSharp` package). It writes one tab-separated line per error; `Program.cs`
describes the format.

It is a .NET project, so it is not part of the Gradle build: it needs a .NET SDK (10 or later) on the `PATH`, and
NuGet access the first time it is built. Its build output goes to `build/`, beside the sources.

```
dotnet build tools/csharp-check -c Release
dotnet tools/csharp-check/build/bin/Release/net10.0/csharp-check.dll <argument file>
```
