package org.e2immu.analyzer.aapi.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnnotatedAPIConfigurationImpl implements AnnotatedAPIConfiguration {

    // use case 1
    private final List<String> analyzedAnnotatedApiDirs;
    // use case 2
    private final String analyzedAnnotatedApiTargetDir;
    // use case 3
    private final List<String> annotatedApiPackages;
    private final String annotatedApiTargetDir;
    private final String annotatedApiTargetPackage;

    private AnnotatedAPIConfigurationImpl(List<String> analyzedAnnotatedApiDirs,
                                          String analyzedAnnotatedApiTargetDir,
                                          List<String> annotatedApiPackages,
                                          String annotatedApiTargetDir,
                                          String annotatedApiTargetPackage) {
        this.analyzedAnnotatedApiDirs = analyzedAnnotatedApiDirs;
        this.analyzedAnnotatedApiTargetDir = analyzedAnnotatedApiTargetDir;
        this.annotatedApiTargetDir = annotatedApiTargetDir;
        this.annotatedApiTargetPackage = annotatedApiTargetPackage;
        this.annotatedApiPackages = annotatedApiPackages;
    }

    @Override
    public List<String> analyzedAnnotatedApiDirs() {
        return analyzedAnnotatedApiDirs;
    }

    @Override
    public String analyzedAnnotatedApiTargetDir() {
        return analyzedAnnotatedApiTargetDir;
    }

    @Override
    public String annotatedApiTargetDir() {
        return annotatedApiTargetDir;
    }

    @Override
    public String annotatedApiTargetPackage() {
        return annotatedApiTargetPackage;
    }

    @Override
    public List<String> annotatedApiPackages() {
        return annotatedApiPackages;
    }

    @Override
    public String toString() {
        return "AnnotatedAPIConfigurationImpl{" +
               "analyzedAnnotatedApiDirs=" + analyzedAnnotatedApiDirs +
               ", analyzedAnnotatedApiTargetDir='" + analyzedAnnotatedApiTargetDir + '\'' +
               ", annotatedApiPackages=" + annotatedApiPackages +
               ", annotatedApiTargetDir='" + annotatedApiTargetDir + '\'' +
               ", annotatedApiTargetPackage='" + annotatedApiTargetPackage + '\'' +
               '}';
    }

    public static class Builder {
        // use case 1
        private final List<String> analyzedAnnotatedApiDirs = new ArrayList<>();
        // use case 2
        private String analyzedAnnotatedApiTargetDir;
        // use case 3
        private String annotatedApiTargetDir;
        private final List<String> annotatedApiPackages = new ArrayList<>();
        private String annotatedApiTargetPackage;

        public Builder setAnalyzedAnnotatedApiTargetDir(String analyzedAnnotatedApiTargetDir) {
            this.analyzedAnnotatedApiTargetDir = analyzedAnnotatedApiTargetDir;
            return this;
        }

        public Builder addAnalyzedAnnotatedApiDirs(String... analyzedAnnotatedApiDirs) {
            this.analyzedAnnotatedApiDirs.addAll(Arrays.asList(analyzedAnnotatedApiDirs));
            return this;
        }

        public Builder addAnnotatedApiPackages(String... annotatedApiPackages) {
            this.annotatedApiPackages.addAll(Arrays.asList(annotatedApiPackages));
            return this;
        }

        public Builder setAnnotatedApiTargetDir(String annotatedApiTargetDir) {
            this.annotatedApiTargetDir = annotatedApiTargetDir;
            return this;
        }

        public Builder setAnnotatedApiTargetPackage(String annotatedApiTargetPackage) {
            this.annotatedApiTargetPackage = annotatedApiTargetPackage;
            return this;
        }

        public AnnotatedAPIConfiguration build() {
            return new AnnotatedAPIConfigurationImpl(List.copyOf(analyzedAnnotatedApiDirs),
                    analyzedAnnotatedApiTargetDir,
                    List.copyOf(annotatedApiPackages), annotatedApiTargetDir, annotatedApiTargetPackage);
        }
    }
}
