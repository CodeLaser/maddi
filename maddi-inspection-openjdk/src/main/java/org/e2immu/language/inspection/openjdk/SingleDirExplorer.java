package org.e2immu.language.inspection.openjdk;

import com.sun.source.util.JavacTask;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;

import javax.tools.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ASTExplorer — a self-contained example of driving javac programmatically
 * and visiting the fully attributed AST (types resolved, symbols attached).
 * <p>
 * Compile:
 * javac --add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
 * ASTExplorer.java
 * <p>
 * Run against any .java file (it does not need to compile cleanly, but types
 * on nodes will only be resolved if javac can find all referenced classes):
 * java --add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
 * ASTExplorer path/to/SomeFile.java
 * <p>
 * What this demonstrates:
 * 1. Programmatic javac invocation via javax.tools.JavaCompiler
 * 2. Driving parse + enter + attribution in one shot with task.analyze()
 * 3. Walking every tree node with TreePathScanner (the right base class)
 * 4. Resolving the TypeMirror of any expression via Trees.getTypeMirror()
 * 5. Resolving the Element (method/field/class) via Trees.getElement()
 * 6. Controlling lifecycle so the JavacTask can be GC'd when you're done
 */
public class SingleDirExplorer {

    private final Runtime runtime;

    public SingleDirExplorer(Runtime runtime) {
        this.runtime = runtime;
    }

    public List<TypeInfo> go(SourceSet sourceSet, String classPathDir) throws Exception {

        Path root = Path.of(sourceSet.uri().toURL().toURI());
        if (!Files.isDirectory(root)) {
            System.err.println("Not a directory: " + root);
            System.err.println("Base directory is " + Path.of(".").toAbsolutePath());
            System.exit(1);
        }

        // Collect every .java file under the root, in a stable order.
        List<File> sources;
        try (Stream<Path> walk = Files.walk(root)) {
            sources = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .sorted()
                    .map(Path::toFile)
                    .toList();
        }

        if (sources.isEmpty()) {
            System.err.println("No .java files found under " + root);
            System.exit(1);
        }
        System.out.printf("Found %d source files.%n%n", sources.size());

        // Collect .jar files if a lib directory was supplied
        List<File> jars = List.of();
        if (classPathDir != null) {
            Path libDir = Path.of(classPathDir);
            if (!Files.isDirectory(libDir)) {
                System.err.println("Not a directory: " + libDir);
                System.exit(1);
            }
            try (Stream<Path> walk = Files.walk(libDir)) {
                jars = walk
                        .filter(p -> p.toString().endsWith(".jar"))
                        .sorted()
                        .map(Path::toFile)
                        .toList();
            }
            System.out.printf("Found %d jar files.%n%n", jars.size());
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("No Java compiler available — run on a JDK, not a JRE.");
            System.exit(1);
        }

        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(null, null, null)) {

            // Set the classpath on the file manager before creating the task.
            // CLASS_PATH covers both the jars we found and anything already
            // on the JVM's own classpath (e.g. your analyzer's own classes).
            // If you want to suppress the latter and use only explicit jars,
            // replace the concatenation with just: jars
            if (!jars.isEmpty()) {
                List<File> classpath = Stream.concat(
                                jars.stream(),
                                Stream.of(System.getProperty("java.class.path")
                                                .split(File.pathSeparator))
                                        .map(File::new)
                                        .filter(File::exists)
                        )
                        .collect(Collectors.toList());

                fileManager.setLocation(StandardLocation.CLASS_PATH, classpath);
            }

            Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjects(sources.toArray(new File[0]));
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

            JavacTask task = (JavacTask) compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    List.of("-proc:none"),
                    null,
                    compilationUnits
            );

            return new ScanCompilationUnits(runtime, diagnostics).scan(task, sourceSet);
        }
    }
}