package org.e2immu.language.inspection.integration;
import static org.e2immu.language.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;

public class ToolChain {

    public static final String[] CLASSPATH_JUNIT = {
            JAR_WITH_PATH_PREFIX + "org/junit/jupiter/api",
            JAR_WITH_PATH_PREFIX + "org/apiguardian/api",
            JAR_WITH_PATH_PREFIX + "org/junit/platform/commons",
            JAR_WITH_PATH_PREFIX + "org/opentest4j"};
    public static final String[] CLASSPATH_SLF4J_LOGBACK = {
            JAR_WITH_PATH_PREFIX + "org/slf4j/event",
            JAR_WITH_PATH_PREFIX + "ch/qos/logback/core",
            JAR_WITH_PATH_PREFIX + "ch/qos/logback/classic"};

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
