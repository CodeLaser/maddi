package org.e2immu.language.inspection.openjdk;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.code.Symbol;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
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
    private JavacTask javacTask;
    private SourceSet sourceSet;
    private TypeData typeData;

    public CommonTest() {
        this.runtime = new RuntimeImpl();
    }

    public List<TypeInfo> scan(Map<String, String> sourcesByClassName, List<File> jars) {
        sourceSet = new SourceSetImpl(
                "source", List.of(),
                URI.create("file:/"),
                StandardCharsets.UTF_8, false, false, false,
                false, false, Set.of(), Set.of());
        try {
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            javacTask = createTask(sourcesByClassName, jars, diagnostics);
            ScanCompilationUnits scanCompilationUnits = new ScanCompilationUnits(runtime, diagnostics);
            typeData = scanCompilationUnits.typeData();

            return scanCompilationUnits.scan(javacTask, sourceSet);
        } catch (IOException io) {
            fail(io);
            return null;
        }
    }

    public TypeInfo loadType(String fqn) {
        Elements elements = javacTask.getElements();
        TypeElement typeElement = elements.getTypeElement(fqn);
        ClassSymbolScanner css = new ClassSymbolScanner(runtime, sourceSet, new FlagHelper(runtime), elements, typeData);
        css.setConvertType(new ConvertType(runtime, css, typeData, _ -> null, null));
        return css.primaryType((Symbol.ClassSymbol) typeElement, true);
    }

    private JavacTask createTask(Map<String, String> sourcesByClassName, List<File> jars,
                                 DiagnosticCollector<JavaFileObject> diagnostics) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, null, null);

        if (!jars.isEmpty()) {
            fm.setLocation(StandardLocation.CLASS_PATH, jars);
        }

        // Wrap each source string in an InMemoryJavaFileObject
        List<JavaFileObject> compilationUnits = sourcesByClassName.entrySet().stream()
                .map(e -> new InMemoryJavaFileObject(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return (JavacTask) compiler.getTask(
                null, fm, diagnostics,
                List.of("-proc:none"),
                null,
                compilationUnits
        );
    }
}
