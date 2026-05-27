package org.e2immu.analyzer.run.scanbuild;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;

import javax.tools.JavaFileManager;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ScanBuildPlugin implements Plugin {

    @Override
    public String getName() {
        return "ScanBuild";
    }

    @Override
    public void init(JavacTask task, String... args) {
        // output file path passed as plugin arg, e.g. -Xplugin:"ScanBuild /tmp/config.json"
        String outputPath = args.length > 0 ? args[0] : "/tmp/javac-config.json";

        try {
            Context context = ((BasicJavacTask) task).getContext();

            Options options = Options.instance(context);
            // Print everything in options to find the right keys
            options.keySet().forEach(k ->
                    System.err.println("option: '" + k + "' = '" + options.get(k) + "'"));

            // Get the file manager via its context key — no cast needed
            JavaFileManager fm = context.get(JavaFileManager.class);

            for (var method : fm.getClass().getMethods()) {
                if (method.getName().toLowerCase().contains("location") ||
                    method.getName().toLowerCase().contains("path") ||
                    method.getName().toLowerCase().contains("class")) {
                    System.err.println("method: " + method.getName()
                                       + " -> " + method.getReturnType().getSimpleName());
                }
            }

         /*   // Convert to lists for JSON output
            List<String> classOutputPaths  = toPaths(classOutput);
            List<String> classPathPaths    = toPaths(classPath);
            List<String> sourcePathPaths   = toPaths(sourcePath);
            List<String> modulePathPaths   = toPaths(modulePath);

            // Collect source files via TaskListener
            List<String> sourceFiles = new ArrayList<>();
            task.addTaskListener(new TaskListener() {
                @Override
                public void started(TaskEvent e) {
                    if (e.getKind() == TaskEvent.Kind.PARSE) {
                        sourceFiles.add(e.getSourceFile().toUri().getPath());
                    }
                }

                @Override
                public void finished(TaskEvent e) {
                    if (e.getKind() != TaskEvent.Kind.COMPILATION) return;
                    writeConfig(outputFile, classOutputPaths, classPathPaths,
                            sourcePathPaths, modulePathPaths, sourceFiles);
                }
            });

*/

        } catch (Exception e) {
            throw new RuntimeException("Problematic", e);
        }
    }

List<String> toPaths(Iterable<? extends Path> paths) {
    if (paths == null) return List.of();
    List<String> result = new ArrayList<>();
    paths.forEach(p -> result.add(p.toAbsolutePath().toString()));
    return result;
}
    void appendPaths(StringBuilder sb, Iterable<? extends File> location) {
        sb.append("[");
        if (location != null) {
            boolean first = true;
            for (File f : location) {
                if (!first) sb.append(", ");
                else first = false;
                sb.append("\"").append(f.getAbsolutePath().replace("\\", "\\\\")).append("\"");
            }
        }
        sb.append("]");
    }
}