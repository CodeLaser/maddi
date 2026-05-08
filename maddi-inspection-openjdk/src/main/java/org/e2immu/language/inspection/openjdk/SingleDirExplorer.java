package org.e2immu.language.inspection.openjdk;

import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
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

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: ASTExplorer <src/main/java>");
            System.exit(1);
        }

        Path root = Path.of(args[0]);
        if (!Files.isDirectory(root)) {
            System.err.println("Not a directory: " + root);
            System.exit(1);
        }

        // Collect every .java file under the root, in a stable order.
        List<File> sources;
        try (Stream<Path> walk = Files.walk(root)) {
            sources = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .sorted()
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }

        if (sources.isEmpty()) {
            System.err.println("No .java files found under " + root);
            System.exit(1);
        }
        System.out.printf("Found %d source files.%n%n", sources.size());

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("No Java compiler available — run on a JDK, not a JRE.");
            System.exit(1);
        }

        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(null, null, null)) {

            Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjects(sources.toArray(new File[0]));

            JavacTask task = (JavacTask) compiler.getTask(
                    null,
                    fileManager,
                    null,
                    List.of("-proc:none"),
                    null,
                    compilationUnits
            );

            Trees trees = Trees.instance(task);

            Iterable<? extends CompilationUnitTree> units = task.parse();
            task.analyze();

            for (CompilationUnitTree unit : units) {
                System.out.println("=== " + unit.getSourceFile().getName() + " ===\n");
                new AnalysisScanner(trees, System.out).scan(unit, null);
            }
        }
    }
}