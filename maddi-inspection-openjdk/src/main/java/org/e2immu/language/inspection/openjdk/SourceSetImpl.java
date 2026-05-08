package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.support.SetOnce;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record SourceSetImpl(
        String name,
        Charset sourceEncoding,
        List<Path> sourceDirectories,
        URI uri,
        boolean test,
        boolean runtimeOnly,
        boolean library,
        boolean externalLibrary,
        boolean partOfJdk,
        Set<String> restrictToPackages,
        Set<SourceSet> dependencies,
        SetOnce<FingerPrint> fingerPrint,
        SetOnce<FingerPrint> analysisFingerPrint) implements SourceSet {

    public SourceSetImpl(String name, URI uri, boolean test,
                         boolean runtimeOnly,
                         boolean library,
                         boolean externalLibrary,
                         boolean partOfJdk) {
        this(name, StandardCharsets.UTF_8, List.of(), uri, test, runtimeOnly, library, externalLibrary, partOfJdk,
                Set.of(), Set.of(), new SetOnce<>(), new SetOnce<>());
    }

    @Override
    public FingerPrint fingerPrintOrNull() {
        return fingerPrint.getOrDefaultNull();
    }

    @Override
    public void setFingerPrint(FingerPrint fingerPrint) {
        this.fingerPrint.set(fingerPrint);
    }

    @Override
    public FingerPrint analysisFingerPrintOrNull() {
        return analysisFingerPrint.getOrDefaultNull();
    }

    @Override
    public void setAnalysisFingerPrint(FingerPrint fingerPrint) {
        this.analysisFingerPrint.set(fingerPrint);
    }

    @Override
    public boolean acceptSource(String packageName, String typeName) {
        return false;
    }

    @Override
    public SourceSet withDependencies(Set<SourceSet> dependencies) {
        return null;
    }

    @Override
    public SourceSet withSourceDirectoriesUri(List<Path> sourceDirectories, URI uri) {
        return null;
    }

    @Override
    public SourceSet withSourceDirectories(List<Path> sourceDirectories) {
        return null;
    }

    @Override
    public void computePriorityDependencies() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<SourceSet, Integer> priorityDependencies() {
        throw new UnsupportedOperationException();
    }
}
