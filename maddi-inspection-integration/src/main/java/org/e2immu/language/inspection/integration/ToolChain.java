package org.e2immu.language.inspection.integration;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.e2immu.language.inspection.integration.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;

public class ToolChain {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToolChain.class);

    public record JRE(int mainVersion, String platformVersion, String vendor, String path, String shortName) {
    }

    public static final List<JRE> JRES = DetectJREs.runSystemCommand();
    public static final Map<String, String> jreShortNameToAnalyzedPackageFiles = DetectJREs.loadJreMapping(JRES);

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

    public static String currentJdkAnalyzedPackages() {
        JRE jre = currentJre();
        if (jre == null) return "";
        String currentJreShortName = jre.shortName();
        String analyzedPackageFile = mapJreShortNameToAnalyzedPackageShortName(currentJreShortName);
        return jdkAnalyzedPackages(analyzedPackageFile);
    }

    public static String mapJreShortNameToAnalyzedPackageShortName(String shortName) {
        return jreShortNameToAnalyzedPackageFiles.getOrDefault(shortName, shortName);
    }

    public static final String RESOURCE_PROTOCOL = "resource:";
    public static final String RESOURCE_PREFIX = RESOURCE_PROTOCOL + "/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles";

    public static String jdkAnalyzedPackages(String jdkSpec) {
        return RESOURCE_PREFIX + "/jdk/" + jdkSpec + ".jar";
    }

    public static String commonLibsAnalyzedPackages() {
        return RESOURCE_PREFIX + "/libs.jar";
    }

    // internal

    public static JRE currentJre() {
        String home = System.getProperty("java.home");
        return JRES.stream().filter(jre -> jre.path.equals(home))
                .findFirst()
                .orElseGet(() -> {
                    LOGGER.warn("Toolchain not found - expected JRE = {}, available JREs = {}\n",
                            home,
                            JRES.stream().map(jre -> jre.path).collect(Collectors.joining(",")));
                    return null;
                });
    }

    public static int currentJdkMainVersion() {
        JRE jre = currentJre();
        return jre == null ? 24 : jre.mainVersion;
    }

    private static final Pattern JDK_PATTERN = Pattern.compile("jdk-([0-9]+(\\.[0-9]+)*)\\.jdk");
    private static final Pattern MAC_OPENJDK_PATTERN = Pattern.compile("openjdk(@\\d+)?/([\\d.]+)/libexec/openjdk.jdk");
    private static final Pattern LINUX_OPENJDK_PATTERN = Pattern.compile("/usr/lib/jvm/java-(\\d+)-openjdk");

    // Linux: jar:file:/usr/lib/jvm/java-23-openjdk-arm64/jmods/java.base.jmod!/classes/java/io/BufferedInputStream.class
    // Mac: jar:file:/opt/homebrew/Cellar/openjdk/23.0.2/libexec/openjdk.jdk/Contents/Home/jmods/java.base.jmod!/classes/java/io/BufferedInputStream.class

    public static String extractJdkName(String jdkHome) {
        Matcher m0 = JDK_PATTERN.matcher(jdkHome);
        if (m0.find()) {
            return "jdk-" + m0.group(1);
        }
        Matcher m = MAC_OPENJDK_PATTERN.matcher(jdkHome);
        if (m.find()) {
            return "openjdk-" + m.group(2);
        }
        Matcher mm = LINUX_OPENJDK_PATTERN.matcher(jdkHome);
        if (mm.find()) {
            return "openjdk-" + mm.group(1);
        }
        return null;
    }

    private static final Pattern JAR_PATTERN = Pattern.compile("/([^/]+)\\.jar!/");

    public static String extractLibraryName(List<TypeInfo> list, boolean prefixWithJdk) {
        LOGGER.info("List has size {}", list.size());
        String uri = list.stream()
                .filter(ti -> ti.compilationUnit().uri() != null)
                .peek(ti -> LOGGER.info("Class {} -> {}", ti, ti.compilationUnit().uri()))
                .map(ti -> ti.compilationUnit().uri().toString())
                .filter(s -> !s.contains("predefined://")).findFirst().orElse(null);
        if (uri != null) {
            String jdkShortHand = extractJdkName(uri);
            if (jdkShortHand != null) {
                return (prefixWithJdk ? "jdk/" : "") + jdkShortHand;
            }
            Matcher m2 = JAR_PATTERN.matcher(uri);
            if (m2.find()) {
                return "libs/" + m2.group(1);
            }
        }
        return "libs/unclassified";
    }
}
