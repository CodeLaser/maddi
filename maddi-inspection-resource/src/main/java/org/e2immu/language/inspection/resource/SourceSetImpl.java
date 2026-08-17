/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.language.inspection.resource;

import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.support.SetOnce;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

import static org.e2immu.language.inspection.api.integration.JavaInspector.TEST_PROTOCOL;

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
    private final boolean isModule;
    private final Set<String> restrictToPackages;
    private final List<SourceSet> dependencies;
    private final String buildUnit;
    private final int sourceRelease;
    private final List<String> addModules;
    private final SetOnce<FingerPrint> fingerPrint = new SetOnce<>();
    private final SetOnce<FingerPrint> analysisFingerPrint = new SetOnce<>();
    private final SetOnce<Map<SourceSet, Integer>> priorityDependencies = new SetOnce<>();

    private SourceSetImpl(String name,
                          List<Path> sourceDirectories, URI uri,
                          Charset sourceEncoding,
                          boolean test, boolean library, boolean externalLibrary, boolean partOfJdk,
                          boolean isModule, boolean runtimeOnly,
                          Set<String> restrictToPackages,
                          List<SourceSet> dependencies,
                          String buildUnit,
                          int sourceRelease,
                          List<String> addModules) {
        this.name = Objects.requireNonNull(name);
        this.buildUnit = buildUnit;
        this.sourceDirectories = sourceDirectories;
        this.uri = Objects.requireNonNull(uri, "Must have a URI in a source set");
        Objects.requireNonNull(uri.getScheme(), "The URI of source set " + name + " must have a non-null scheme");
        this.sourceEncoding = sourceEncoding;
        this.test = test;
        this.library = library;
        this.externalLibrary = externalLibrary;
        this.partOfJdk = partOfJdk;
        this.runtimeOnly = runtimeOnly;
        this.isModule = isModule;
        this.restrictToPackages = restrictToPackages;
        this.dependencies = dependencies;
        this.sourceRelease = sourceRelease;
        this.addModules = addModules == null ? List.of() : List.copyOf(addModules);

        assert !runtimeOnly || externalLibrary : "Runtime-only can only be true for external libraries: " + name;
        assert !partOfJdk || externalLibrary : "Parts of the JDK are also external libraries: " + name;
        assert !partOfJdk || isModule : "Parts of the JDK are always modules: " + name;
    }

    public static SourceSet javaBase() {
        return new Builder().setName("java.base").setUri(URI.create("file:/"))
                .setLibrary(true)
                .setExternalLibrary(true).setPartOfJdk(true).setModule(true).build();
    }

    public static SourceSet jdkModule(String name) {
        return new Builder().setName(name).setUri(URI.create("file:/"))
                .setLibrary(true)
                .setExternalLibrary(true).setPartOfJdk(true).setModule(true).build();
    }

    public static SourceSet sourceSetModuleOf(Class<?> clazz, SourceSet... dependencies) {
        return sourceSetOf(clazz, true, dependencies);
    }

    public static SourceSet sourceSetOf(Class<?> clazz, SourceSet... dependencies) {
        return sourceSetOf(clazz, false, dependencies);
    }

    private static SourceSet sourceSetOf(Class<?> clazz, boolean isModule, SourceSet... dependencies) {
        try {
            URI uri = clazz.getProtectionDomain().getCodeSource().getLocation().toURI();
            return new Builder().setName(tail(uri)).setUri(uri)
                    .setModule(isModule)
                    .setLibrary(true)
                    .setExternalLibrary(true)
                    .setDependencies(Arrays.stream(dependencies).toList())
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The classpath archives javac accepts: a zip file by any other name.
     *
     * <p>⛔⛔ <b>{@code .jar} IS A CONVENTION, NOT THE CONTRACT, AND READING IT AS THE CONTRACT LOSES A WHOLE
     * LIBRARY IN SILENCE.</b> Apache BookKeeper publishes {@code circe-checksum} and {@code cpu-affinity} as
     * <b>{@code .nar}</b> (a jar with native libraries beside the classes), and Apache Pulsar puts both straight
     * on javac's {@code -classpath}. Measured on pulsar 5.0.0-M1, 2026-08-12: the two files hold 59 and 12 class
     * files, {@code CompileListToSourceSets} matched neither {@code endsWith(".jar")} nor {@code isDirectory()},
     * so they reached no classpath at all — and javac then reported
     * <i>"package com.scurrilous.circe.checksum does not exist"</i> for the four source sets that import it.
     *
     * <p>⛔ <b>THE COST IS NOT THE FOUR FILES.</b> javac stops attributing after the first errors, so every
     * compilation unit behind them comes out with null symbols: <b>1,831 compilation units dropped</b>
     * (pulsar-broker 1,226 of 1,527), reported as 1,826 errors of which 1,820 were one
     * {@code UnsupportedOperationException} — <i>naming the consumer, never the cause</i>, exactly as
     * {@code AnnotationProcessorOutput} records for its own hole.
     */
    public static final Set<String> ARCHIVE_EXTENSIONS = Set.of(".jar", ".nar", ".zip");

    /**
     * Whether a classpath part is an archive maddi should open as a jar. Matched on the extension only: the
     * caller has a path, not necessarily an existing file (tests build configurations for files that are not
     * there).
     */
    public static boolean isArchive(String pathOrName) {
        if (pathOrName == null) return false;
        int dot = pathOrName.lastIndexOf('.');
        return dot >= 0 && ARCHIVE_EXTENSIONS.contains(pathOrName.substring(dot).toLowerCase(Locale.ROOT));
    }

    public static String tail(URI uri) {
        String toString = uri.toString();
        int last = toString.lastIndexOf('/');
        String name = toString.substring(last + 1);
        assert isArchive(name) : "not a classpath archive: " + name;
        return name;
    }

    public static SourceSet testProtocolSourceSet() {
        return new Builder().setName(TEST_PROTOCOL).setUri(URI.create("file:/")).build();
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
    public String buildUnit() {
        return buildUnit;
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
    public boolean isModule() {
        return isModule;
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
    public int sourceRelease() {
        return sourceRelease;
    }

    @Override
    public List<String> addModules() {
        return addModules;
    }

    @Override
    public List<SourceSet> dependencies() {
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
        return new SourceSetImpl(name, paths, uri, sourceEncoding, test, library, externalLibrary, partOfJdk,
                isModule, runtimeOnly, restrictToPackages, dependencies, buildUnit, sourceRelease, addModules);
    }

    @Override
    public SourceSet withSourceDirectoriesUri(List<Path> sourceDirectories, URI uri) {
        return new SourceSetImpl(name, sourceDirectories, uri, sourceEncoding, test, library, externalLibrary, partOfJdk,
                isModule, runtimeOnly, restrictToPackages, dependencies, buildUnit, sourceRelease, addModules);
    }

    @Override
    public SourceSet withDependencies(List<SourceSet> dependencies) {
        return new SourceSetImpl(name, sourceDirectories, uri, sourceEncoding, test, library,
                externalLibrary, partOfJdk, isModule, runtimeOnly, restrictToPackages, dependencies, buildUnit, sourceRelease, addModules);
    }

    @Override
    public Map<SourceSet, Integer> priorityDependencies() {
        assert priorityDependencies.isSet() : "Priority dependencies of source set " + name + " have not yet been set";
        return priorityDependencies.get();
    }

    @Override
    public void computePriorityDependencies() {
        if (!priorityDependencies.isSet()) {
            Map<SourceSet, Integer> map = new HashMap<>();
            recursiveDependencies(map, 1);
            priorityDependencies.set(Map.copyOf(map));
        }
    }

    void recursiveDependencies(Map<SourceSet, Integer> result, int distance) {
        for (SourceSet dependency : dependencies) {
            Integer current = result.get(dependency);
            if (current == null || current > distance) {
                result.put(dependency, distance);
                ((SourceSetImpl) dependency).recursiveDependencies(result, distance + 1);
            }
        }
    }

    public static class Builder {
        private String name;
        private List<Path> sourceDirectories = List.of();
        private URI uri;
        private Charset sourceEncoding = StandardCharsets.UTF_8;
        private boolean test;
        private boolean library;
        private boolean externalLibrary;
        private boolean partOfJdk;
        private boolean runtimeOnly;
        private boolean isModule;
        private Set<String> restrictToPackages = Set.of();
        private List<SourceSet> dependencies = List.of();
        private String buildUnit;
        private int sourceRelease;
        private List<String> addModules = List.of();

        public Builder() {
        }

        public Builder(SourceSet set) {
            name = set.name();
            buildUnit = set.buildUnit();
            sourceDirectories = set.sourceDirectories();
            uri = set.uri();
            sourceEncoding = set.sourceEncoding();
            test = set.test();
            library = set.library();
            externalLibrary = set.externalLibrary();
            partOfJdk = set.partOfJdk();
            isModule = set.isModule();
            runtimeOnly = set.runtimeOnly();
            restrictToPackages = set.restrictToPackages();
            dependencies = set.dependencies();
            // ⛔ Field by field, so a new one added above and forgotten here is dropped SILENTLY -- unlike the
            // positional constructor, which stops compiling. That is what the runtimeOnly test guards against
            // ("losing it silently widens that classpath"), and these two are in the same position: a dropped
            // sourceRelease reinstates "whatever JDK maddi runs on" for that set, invisibly.
            sourceRelease = set.sourceRelease();
            addModules = set.addModules();
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setBuildUnit(String buildUnit) {
            this.buildUnit = buildUnit;
            return this;
        }

        public Builder setSourceDirectories(List<Path> sourceDirectories) {
            this.sourceDirectories = sourceDirectories;
            return this;
        }

        public Builder setUri(URI uri) {
            this.uri = uri;
            return this;
        }

        public Builder setDependencies(List<SourceSet> dependencies) {
            this.dependencies = dependencies;
            return this;
        }

        public Builder setExternalLibrary(boolean externalLibrary) {
            this.externalLibrary = externalLibrary;
            return this;
        }

        public Builder setLibrary(boolean library) {
            this.library = library;
            return this;
        }

        public Builder setModule(boolean module) {
            isModule = module;
            return this;
        }

        public Builder setPartOfJdk(boolean partOfJdk) {
            this.partOfJdk = partOfJdk;
            return this;
        }

        public Builder setRestrictToPackages(Set<String> restrictToPackages) {
            this.restrictToPackages = restrictToPackages;
            return this;
        }

        public Builder setRuntimeOnly(boolean runtimeOnly) {
            this.runtimeOnly = runtimeOnly;
            return this;
        }

        public Builder setSourceEncoding(Charset sourceEncoding) {
            this.sourceEncoding = sourceEncoding;
            return this;
        }

        public Builder setTest(boolean test) {
            this.test = test;
            return this;
        }

        /** The set's own {@code javac --release}; {@code <= 0} leaves it unstated. */
        public Builder setSourceRelease(int sourceRelease) {
            this.sourceRelease = sourceRelease;
            return this;
        }

        /** The set's own {@code javac --add-modules}, module names only. */
        public Builder setAddModules(List<String> addModules) {
            this.addModules = addModules == null ? List.of() : List.copyOf(addModules);
            return this;
        }

        public SourceSet build() {
            return new SourceSetImpl(name, sourceDirectories, uri, sourceEncoding, test, library,
                    externalLibrary, partOfJdk, isModule, runtimeOnly, restrictToPackages, dependencies, buildUnit, sourceRelease, addModules);
        }
    }
}
