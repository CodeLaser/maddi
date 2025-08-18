package org.e2immu.language.cst.api.element;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Possible situations.
 * <p>
 * There are quite a few external libraries that contain java sources in the .jar file.
 * If an external library has no sources, they can be generated using a decompiler. Either way, they should end up
 * in the sourceDirectory() for the modification analyzer to be able to produce Annotated-API files.
 */
public interface SourceSet {

    Charset sourceEncoding();

    String name();

    /**
     * If this source set represents sources, this path points to a directory structure that contain the sources.
     * In the case of Java, the directory structure must be compatible with the package of the compilation unit.
     * <p>
     * If this source set represents an external library, this path is either <code>null</code>, or points to
     * the directory where sources have been expanded or computed using a decompiler.
     * The location of the library (jar) is given by the <code>uri()</code>.
     * <p>
     *
     * @return a path representing a directory containing source files
     */
    List<Path> sourceDirectories();

    /**
     * Valid URI with a non-null scheme.
     * <ul>
     *     <li>source directory: 'file' scheme, logically identical to <code>sourceDirectory()</code></li>
     *     <li>external jar on file system: a file with the 'file' scheme, name ends in .jar</li>
     *     <li>a test-protocol entry in a fqn->source map: 'test-protocol' scheme </li>
     * </ul>
     *
     * <p>
     * Some schemes have a special meaning, and will be intercepted: "test-protocol", "jar-on-classpath", "jmod".
     *
     * @return this source set's URI. Cannot be null.
     */
    URI uri();

    default boolean parsedFromSource() {
        return !externalLibrary();
    }

    boolean test();

    /**
     * only relevant when external library is true
     *
     * @return if the source set is a library which is needed at runtime, but not at compile time.
     */
    boolean runtimeOnly();

    boolean library();

    boolean externalLibrary();

    boolean partOfJdk();

    Set<String> restrictToPackages();

    // which sourceSets must be present for this source set to compile/run/resolve?
    Set<SourceSet> dependencies();

    /**
     * Used to determine whether the source of any of the types in this source set has changed.
     * Throws an error when not yet set.
     * <p>
     * The value may be computed from the sources in the <code>path</code>, or from any jar file that is their origin.
     */
    FingerPrint fingerPrintOrNull();

    /**
     * can be set only once.
     *
     * @param fingerPrint the source fingerprint
     */
    void setFingerPrint(FingerPrint fingerPrint);

    /**
     * Used to determine whether the analysis of the whole source set has changed.
     * If so, dependent source sets may have to be re-analyzed.
     * This is typically implemented using a setOnce object since the source set has to be in place before
     * the analysis can take place.
     */
    FingerPrint analysisFingerPrintOrNull();

    void setAnalysisFingerPrint(FingerPrint fingerPrint);

    // helper methods

    boolean acceptSource(String packageName, String typeName);

    SourceSet withDependencies(Set<SourceSet> dependencies);

    SourceSet withSourceDirectoriesUri(List<Path> sourceDirectories, URI uri);

    SourceSet withSourceDirectories(List<Path> sourceDirectories);

    // this is a set-once!
    void setModuleInfo(ModuleInfo moduleInfo);

    ModuleInfo moduleInfo();

    Set<SourceSet> recursiveDependenciesSameExternal();
}
