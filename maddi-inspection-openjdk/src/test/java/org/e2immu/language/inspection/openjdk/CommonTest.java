package org.e2immu.language.inspection.openjdk;

import com.sun.source.util.JavacTask;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

public class CommonTest {

    protected final Runtime runtime;

    public CommonTest() {
        this.runtime = new RuntimeImpl();
    }

    public List<TypeInfo> scan(Map<String, String> sourcesByClassName, List<File> jars) {
        SourceSet sourceSet = new SourceSetImpl(
                "source", List.of(),
                URI.create("file:/"),
                StandardCharsets.UTF_8, false, false, false,
                false, false, Set.of(), Set.of());
        try {
            JavacTask task = createTask(sourcesByClassName, jars);
            return new ScanCompilationUnits(runtime).scan(task, sourceSet);
        } catch (IOException io) {
            fail(io);
            return null;
        }
    }

    private JavacTask createTask(Map<String, String> sourcesByClassName, List<File> jars) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null);

        if (!jars.isEmpty()) {
            fm.setLocation(StandardLocation.CLASS_PATH, jars);
        }

        // Wrap each source string in an InMemoryJavaFileObject
        List<JavaFileObject> compilationUnits = sourcesByClassName.entrySet().stream()
                .map(e -> new InMemoryJavaFileObject(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return (JavacTask) compiler.getTask(
                null, fm, null,
                List.of("-proc:none"),
                null,
                compilationUnits
        );
    }
}
