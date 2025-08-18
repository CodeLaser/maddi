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

Annotated APIs
--------------

- `analyzedAnnotatedApiDirs` (list of strings): 
  - When absent, analyzed annotated APIs are read from the default location.
  - When present, they are read from the locations specified here.
  - When the first entry in this list starts with '+', the default locations are kept. 
  - When present and empty, no analyzed annotated APIs are read.

- `annotatedApiSourcePackages` (list of strings): 
  - When included, treat part of the analyzer's input as annotated API sources to be processed.
  - Each entry is a package name. Sources in this package will be processed.
  - When an entry ends in a dot, all sub-packages are included as well.

- `analyzedAnnotatedApiTargetDirectory` (single string):
  - Only relevant when `annotatedApiSourcePackages` is present. 
  - Absent means that the analyzed versions are not written out. 
  - Present defines where they will be written to.

- `annotatedApiTargetDirectory` (single string):
  - When present, write AAPI skeletons to this directory for all active source packages.
  - Merge strategy: append!

  
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
