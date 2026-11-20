# maddi-aapi-archive

The annotated-API hint sources (`src/main/java/io/codelaser/maddi/aapi/archive/jdk` and `.../archive/libs`) and the
analysed results compiled from them (`src/main/resources`).

## Keep the hint sources' imports and qualified names as they are

Exclude these sources from any import clean-up, import reordering or shortening of qualified names.

- **Units name library types in full.** Many units name their library's types in full and import none:
  `OrgAssertjCoreApi`, `OrgHamcrest`, `OrgMockito*`, `OrgApacheCommonsIo`, the Jackson units, and the Kotlin units
  that import nothing from `kotlin.*`.
  - That is what lets them compile on the shared factory of `maddi-aapi-parser`'s tests, which does not carry those
    libraries' jars.
  - If an import of a library type is added, javac drops the unit there, and `TestParseAnalyzeWrite` counts fewer
    units.
- **Hints must not change.** A rewrite of the jdk units may compile and still change the hints. Any edit there needs
  `compileAnalysisHints` rerun with an empty diff of the committed results; see `TestAnalysisHintsCompiler`.

The same applies to the hint-shaped test sources in maddi-mod's
`maddi-aapi-parser/src/test/java/io/codelaser/maddi/aapi/parser/example`.
