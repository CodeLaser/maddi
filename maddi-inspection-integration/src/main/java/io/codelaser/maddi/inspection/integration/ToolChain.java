package org.e2immu.language.inspection.integration;

import org.e2immu.language.inspection.resource.AnalyzedApiClassPath;

import static org.e2immu.language.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;

public class ToolChain {

    /**
     * @deprecated the annotated-API library class-paths now live in {@link AnalyzedApiClassPath}, a single-purpose
     * companion to the archive that consumers can depend on without pulling in the rest of {@code ToolChain}. These
     * aliases forward to it while {@code ToolChain} is phased out; prefer {@link AnalyzedApiClassPath#JUNIT}.
     */
    @Deprecated
    public static final String[] CLASSPATH_JUNIT = AnalyzedApiClassPath.JUNIT;
    /** @deprecated use {@link AnalyzedApiClassPath#SLF4J_LOGBACK}; see {@link #CLASSPATH_JUNIT}. */
    @Deprecated
    public static final String[] CLASSPATH_SLF4J_LOGBACK = AnalyzedApiClassPath.SLF4J_LOGBACK;

    public static final String CLASSPATH_INTELLIJ_LANG = JAR_WITH_PATH_PREFIX + "org/intellij/lang/annotations";

    public static String[] CLASSPATH_E2IMMU = {
            JAR_WITH_PATH_PREFIX + "org/parsers/java/ast",
            JAR_WITH_PATH_PREFIX + "org/e2immu/util/internal/util",
            JAR_WITH_PATH_PREFIX + "org/e2immu/util/internal/graph",
            JAR_WITH_PATH_PREFIX + "org/e2immu/language/cst/impl/analysis",
            JAR_WITH_PATH_PREFIX + "org/e2immu/language/cst/api",
            JAR_WITH_PATH_PREFIX + "org/e2immu/language/cst/io",
            JAR_WITH_PATH_PREFIX + "org/e2immu/language/cst/imp/element",
            JAR_WITH_PATH_PREFIX + "org/e2immu/language/cst/print",
            JAR_WITH_PATH_PREFIX + "org/e2immu/language/parser/java",
            JAR_WITH_PATH_PREFIX + "org/e2immu/language/inspection/api",
            JAR_WITH_PATH_PREFIX + "org/e2immu/language/inspection/impl/parser",
            JAR_WITH_PATH_PREFIX + "org/e2immu/language/inspection/integration",
            JAR_WITH_PATH_PREFIX + "org/e2immu/language/inspection/resource",
            JAR_WITH_PATH_PREFIX + "org/e2immu/analyzer/modification/common",
            JAR_WITH_PATH_PREFIX + "org/e2immu/analyzer/modification/io",
            JAR_WITH_PATH_PREFIX + "org/e2immu/analyzer/modification/prepwork",
            JAR_WITH_PATH_PREFIX + "org/e2immu/analyzer/modification/linkedvariables"
    };
}
