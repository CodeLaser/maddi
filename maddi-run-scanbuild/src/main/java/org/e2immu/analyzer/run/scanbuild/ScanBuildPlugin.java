package org.e2immu.analyzer.run.scanbuild;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.Context;

import javax.tools.JavaFileManager;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

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

// Get the file manager via its context key — no cast needed
            JavaFileManager fm = context.get(JavaFileManager.class);

// Then get locations via reflection to avoid the cast issue entirely
            StandardJavaFileManager sfm = unwrap(fm);
            StringBuilder json = new StringBuilder("{\n");

            // Source directories
            json.append("  \"sourcePaths\": ");
            appendPaths(json, sfm.getLocation(StandardLocation.SOURCE_PATH));

            // Class output directory
            json.append(",\n  \"classOutput\": ");
            appendPaths(json, sfm.getLocation(StandardLocation.CLASS_OUTPUT));

            // Classpath — jars and class directories
            json.append(",\n  \"classPath\": ");
            appendPaths(json, sfm.getLocation(StandardLocation.CLASS_PATH));

            // Module path if used
            json.append(",\n  \"modulePath\": ");
            appendPaths(json, sfm.getLocation(StandardLocation.MODULE_PATH));

            // Source files being compiled — from the task listener
            // (not available at init time, need a task listener for this)
            json.append(",\n  \"sourceFiles\": []");

            json.append("\n}");

            Files.writeString(Path.of(outputPath), json.toString());

        } catch (Exception e) {
            System.err.println("ScanBuild: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private StandardJavaFileManager unwrap(JavaFileManager fm) {
        // Unwrap ClientCodeWrapper by following the 'clientJFM' field
        try {
            var field = fm.getClass().getDeclaredField("clientJFM");
            field.setAccessible(true);
            JavaFileManager inner = (JavaFileManager) field.get(fm);
            if (inner instanceof StandardJavaFileManager sfm) return sfm;
            return unwrap(inner); // recurse in case of multiple wrapping layers
        } catch (NoSuchFieldException e) {
            // Not wrapped — try direct cast
            if (fm instanceof StandardJavaFileManager sfm) return sfm;
            throw new RuntimeException("Cannot unwrap: " + fm.getClass());
        } catch (Exception e) {
            throw new RuntimeException("Unwrap failed", e);
        }
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