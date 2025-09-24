package org.e2immu.language.inspection.resource;

import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.support.SetOnce;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SourceSetImpl implements SourceSet {
    private final String name;
    private final List<Path> sourceDirectories;
    private final URI uri;
    private final Charset sourceEncoding;
    private final boolean test;
    private final boolean library;
    private final boolean externalLibrary;
    private final boolean partOfJdk;
    private final boolean runtimeOnly;
    private final Set<String> restrictToPackages;
    private final Set<SourceSet> dependencies;
    private final SetOnce<FingerPrint> fingerPrint = new SetOnce<>();
    private final SetOnce<FingerPrint> analysisFingerPrint = new SetOnce<>();

    public SourceSetImpl(String name,
                         List<Path> sourceDirectories, URI uri,
                         Charset sourceEncoding,
                         boolean test, boolean library, boolean externalLibrary, boolean partOfJdk, boolean runtimeOnly,
                         Set<String> restrictToPackages,
                         Set<SourceSet> dependencies) {
        this.name = Objects.requireNonNull(name);
        this.sourceDirectories = sourceDirectories;
        this.uri = Objects.requireNonNull(uri);
        Objects.requireNonNull(uri.getScheme(), "The URI of source set " + name + " must have a non-null scheme");
        this.sourceEncoding = sourceEncoding;
        this.test = test;
        this.library = library;
        this.externalLibrary = externalLibrary;
        this.partOfJdk = partOfJdk;
        this.runtimeOnly = runtimeOnly;
        this.restrictToPackages = restrictToPackages;
        this.dependencies = dependencies;

        assert !runtimeOnly || externalLibrary : "Runtime-only can only be true for external libraries: " + name;
        assert !partOfJdk || externalLibrary : "Parts of the JDK are also external libraries: " + name;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SourceSetImpl sourceSet)) return false;
        return Objects.equals(name, sourceSet.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        String code = partOfJdk ? "[jdk]" : externalLibrary ? "[external]" : library ? "[library]" : test ? "[test]" : "";
        String pathString = sourceDirectories == null ? "<no source dir>"
                : sourceDirectories.size() == 1 ? sourceDirectories.getFirst().toString() : sourceDirectories.toString();
        return name + code + (pathString.equals(name) ? "" : ":" + pathString);
    }

    @Override
    public Charset sourceEncoding() {
        return sourceEncoding;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<Path> sourceDirectories() {
        return sourceDirectories;
    }

    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public boolean test() {
        return test;
    }

    @Override
    public boolean library() {
        return library;
    }

    @Override
    public boolean externalLibrary() {
        return externalLibrary;
    }

    @Override
    public boolean partOfJdk() {
        return partOfJdk;
    }

    @Override
    public boolean runtimeOnly() {
        return runtimeOnly;
    }

    @Override
    public Set<String> restrictToPackages() {
        return restrictToPackages;
    }

    @Override
    public Set<SourceSet> dependencies() {
        return dependencies;
    }

    @Override
    public FingerPrint fingerPrintOrNull() {
        return fingerPrint.getOrDefaultNull();
    }

    @Override
    public void setFingerPrint(FingerPrint fingerPrint) {
        if (this.fingerPrint.isSet()) {
            if (!fingerPrint.equals(this.fingerPrint.get())) {
                throw new UnsupportedOperationException("Trying to overwrite: " + this.fingerPrint.get() + "->" + fingerPrint);
            }
        } else {
            this.fingerPrint.set(fingerPrint);
        }
    }

    @Override
    public FingerPrint analysisFingerPrintOrNull() {
        return analysisFingerPrint.getOrDefaultNull();
    }

    @Override
    public void setAnalysisFingerPrint(FingerPrint fingerPrint) {
        analysisFingerPrint.set(fingerPrint);
    }

    @Override
    public boolean acceptSource(String packageName, String typeName) {
        if (restrictToPackages == null || restrictToPackages.isEmpty()) return true;
        for (String packageString : restrictToPackages) {
            if (packageString.endsWith(".")) {
                if (packageName.startsWith(packageString) ||
                    packageName.equals(packageString.substring(0, packageString.length() - 1))) {
                    return true;
                }
            } else if (packageName.equals(packageString) || packageString.equals(packageName + "." + typeName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public SourceSet withSourceDirectories(List<Path> paths) {
        return new SourceSetImpl(name, paths, uri, sourceEncoding, test, library, externalLibrary, partOfJdk, runtimeOnly,
                restrictToPackages, dependencies);
    }

    @Override
    public SourceSet withSourceDirectoriesUri(List<Path> sourceDirectories, URI uri) {
        return new SourceSetImpl(name, sourceDirectories, uri, sourceEncoding, test, library, externalLibrary, partOfJdk,
                runtimeOnly, restrictToPackages, dependencies);
    }

    @Override
    public SourceSet withDependencies(Set<SourceSet> dependencies) {
        return new SourceSetImpl(name, sourceDirectories, uri, sourceEncoding, test, library,
                externalLibrary, partOfJdk, runtimeOnly, restrictToPackages, dependencies);
    }

    @Override
    public Set<SourceSet> recursiveDependenciesSameExternal() {
        Set<SourceSet> result = new HashSet<>();
        recursiveDependencies(result, externalLibrary);
        return result;
    }

    void recursiveDependencies(Set<SourceSet> result, boolean external) {
        if (this.externalLibrary == external && result.add(this)) {
            if (dependencies != null) {
                for (SourceSet set : dependencies) {
                    ((SourceSetImpl) set).recursiveDependencies(result, external);
                }
            }
        }
    }
}
