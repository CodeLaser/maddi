Gradle plugin
=============

The primary goal of the gradle plugin is to provide the inspector and analyzer with a proper classpath,
and a set of source directories.
These cannot be set as plugin options.


Input sources
-------------

- `sourcePackages` (list of strings):
  - which source packages to analyze.
  - If absent, all sources are analyzed.
  - When an entry ends in a dot, all sub-packages are included.

- `testSourcePackages` (list of strings):
  - Similar to `sourcePackages`. The difference is made only for some analyses.

- `analyzedSourcesDir` (single string):
  - where to write the results of analysis of sources
  - if absent, do not write out result of analysis
- `analyzedTestSourcesDir` (single string):
  - similar to `analyzedSourcesDir`

- `jmods` (list of strings): 
  - which Java modules to add to the classpath
  - when absent, the default modules are added. This is a very short list.

Analysis hints
--------------

Analysis hints are hand-written `.java` files that annotate a library's API surface (`@Immutable`,
`@Container`, `@NotNull`, …) for types the analyzer can't see the source of. Compiling them yields
*analysis results* (`.json`), the precomputed analysis values loaded when analyzing code that uses those
libraries.

- `preloadAnalysisResultsDirs` (list of strings):
  - Directories of pre-computed analysis results (`.json`) to load for library types (use case 1).
  - When absent, results are read from the default location; when present and empty, none are read.

- `hintsPackages` (list of strings):
  - Restricts which packages of hint sources are processed (use cases 2 and 3).
  - When an entry ends in a dot, all sub-packages are included as well.

- `analysisResultsTargetDir` (single string):
  - Where to write the compiled analysis results (`.json`) when processing hint sources (use case 2).
  - Absent means the results are not written out.

- `updatedHintsDir` (single string):
  - Where to write updated analysis-hint (`.java`) files for the processed packages (use case 3).

- `updatedHintsPackage` (single string):
  - Target package for the written hint files.

  
Analysis
--------

- `incrementalAnalysis` (boolean): when `true`, build on all information already present in `analyzed(Test)SourcesDirs`
- `analysisSteps` (list of strings): 
  - describes which analysis steps to carry out. 
  - If absent, all steps are executed.


General options
---------------

- `sourceEncoding` (single string): alternative source encoding, default is UTF-8
- `jre` (single string): alternative JRE location
