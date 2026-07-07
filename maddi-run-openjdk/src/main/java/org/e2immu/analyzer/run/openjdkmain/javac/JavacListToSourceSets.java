package org.e2immu.analyzer.run.openjdkmain.javac;

import org.e2immu.analyzer.run.config.compile.CompileListToSourceSets;

/**
 * The javac view of the shared {@link CompileListToSourceSets} engine. All logic now lives in the superclass
 * (generalized so the kotlin front-end reuses it); this subclass exists only to keep the historical name and
 * its inherited nested types ({@code JavacListToSourceSets.Result} / {@code .JSourceSet}) available to callers.
 */
public class JavacListToSourceSets extends CompileListToSourceSets {
}
