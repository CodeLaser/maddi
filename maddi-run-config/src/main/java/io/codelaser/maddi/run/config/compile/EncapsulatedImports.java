package io.codelaser.maddi.run.config.compile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The {@code --add-exports} a build at {@code -source 8} or lower never had to write.
 *
 * <p>{@code -source N} is not {@code --release N} (see {@link CompileInvocation#effectiveRelease()}), and for
 * {@code N <= 8} it says one thing more: <b>javac does not enforce module encapsulation</b>. The sources sit
 * in no module and every package of the JDK is readable, {@code sun.security.krb5} as much as
 * {@code java.util}. The parse runs on the current JDK at its own language level, where that package is not
 * exported: "package sun.security.krb5 does not exist", one error, javac stops attributing, and every unit of
 * the set is dropped -- a build that is green, read as a module that does not compile. Met on the first Maven
 * project configured from a compile log (Apache DolphinScheduler: {@code -source 1.8 -target 1.8}, 111
 * source sets, one of them with a Kerberos client; 2026-09-21).
 *
 * <p>What the build had for free, the set states explicitly: one {@code <module>/<package>=ALL-UNNAMED} per
 * encapsulated JDK package its sources <em>import</em>. The parse already knows what to do with a set that
 * opens a system module: it compiles it against the running JDK rather than under {@code --release}.
 *
 * <p>⚠ By import, so a package reached only through a fully qualified name in a body is not found; and by
 * the JDK that runs the configuration step, so a package that has since been removed has no module to name.
 * Both leave the set where it was before this class existed.
 */
final class EncapsulatedImports {

    private static final Pattern IMPORT = Pattern.compile(
            "^\\s*import\\s+(static\\s+)?([\\p{L}_$][\\p{L}\\p{N}_$]*(?:\\.[\\p{L}_$][\\p{L}\\p{N}_$]*)*)\\s*(\\.\\*)?\\s*;");

    private EncapsulatedImports() {
    }

    /** Whether the invocation compiled without encapsulation: a {@code -source} of 8 or lower, and no {@code --release}. */
    static boolean applies(CompileInvocation invocation) {
        return invocation.release() <= 0 && invocation.sourceRelease() > 0 && invocation.sourceRelease() <= 8;
    }

    /** One {@code module/package=ALL-UNNAMED} per encapsulated JDK package imported below {@code sourceDirs}; sorted. */
    static List<String> addExportsFor(List<Path> sourceDirs) {
        Set<String> exports = new TreeSet<>();
        for (Path dir : sourceDirs) {
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> files = Files.walk(dir)) {
                files.filter(p -> p.toString().endsWith(".java") && Files.isRegularFile(p))
                        .forEach(p -> collect(p, exports));
            } catch (IOException | UncheckedIOException e) {
                // an unreadable directory contributes nothing; the parse will say what it has to
            }
        }
        return List.copyOf(exports);
    }

    private static void collect(Path file, Set<String> exports) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.ISO_8859_1);
        } catch (IOException | UncheckedIOException e) {
            return;
        }
        for (String line : lines) {
            Matcher m = IMPORT.matcher(line);
            if (!m.find()) {
                // imports end where the first type begins; a cheap stop, not a parser
                if (line.contains("class ") || line.contains("interface ") || line.contains("enum ")
                    || line.contains("record ")) break;
                continue;
            }
            String name = m.group(2);
            boolean onDemand = m.group(3) != null;
            // `import a.b.C;` names package a.b; `import a.b.*;` names a.b itself; a static or nested import
            // names something deeper, so every prefix is tried, longest first
            List<String> candidates = new ArrayList<>();
            String at = onDemand ? name : parent(name);
            while (at != null) {
                candidates.add(at);
                at = parent(at);
            }
            for (String candidate : candidates) {
                String export = encapsulated(candidate);
                if (export != null) {
                    exports.add(export);
                    break;
                }
                if (exportedByTheJdk(candidate)) break;
            }
        }
    }

    private static String parent(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? null : name.substring(0, dot);
    }

    /** {@code module/package=ALL-UNNAMED} when a JDK module holds the package and does not export it to everyone. */
    static String encapsulated(String packageName) {
        for (Module module : ModuleLayer.boot().modules()) {
            if (module.getPackages().contains(packageName)) {
                return module.isExported(packageName) ? null : module.getName() + "/" + packageName + "=ALL-UNNAMED";
            }
        }
        return null;
    }

    private static boolean exportedByTheJdk(String packageName) {
        for (Module module : ModuleLayer.boot().modules()) {
            if (module.getPackages().contains(packageName)) return true;
        }
        return false;
    }
}
