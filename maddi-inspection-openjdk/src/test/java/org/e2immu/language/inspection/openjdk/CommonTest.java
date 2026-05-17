package org.e2immu.language.inspection.openjdk;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Types;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.Formatter;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.info.ImportComputerImpl;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.cst.print.FormattingOptionsImpl;
import org.e2immu.language.cst.print.formatter2.Formatter2Impl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.resource.SourceSetImpl;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

public class CommonTest {

    protected final Runtime runtime;
    private JavacTask javacTask;
    private SourceSet sourceSet;
    private ClassSymbolScanner classSymbolScanner;

    public CommonTest() {
        this.runtime = new RuntimeImpl();
    }

    public TypeInfo scan(String fqn, String content) {
        return scan(false, Map.of(fqn, content), List.of())
                .stream().map(i -> (TypeInfo) i).findFirst().orElseThrow();
    }

    public Map<String, TypeInfo> scan(boolean ignoreErrorss, String... fqnContentPairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < fqnContentPairs.length; i += 2) {
            map.put(fqnContentPairs[i], fqnContentPairs[i + 1]);
        }
        List<Info> typeInfoList = scan(ignoreErrorss, map, List.of());
        return typeInfoList.stream()
                .map(i -> (TypeInfo) i)
                .collect(Collectors.toUnmodifiableMap(Info::fullyQualifiedName, ti -> ti));
    }

    public List<Info> scan(boolean ignoreErrors, Map<String, String> sourcesByClassName, List<File> jars) {
        sourceSet = new SourceSetImpl(
                "source", List.of(),
                URI.create("file:/"),
                StandardCharsets.UTF_8, false, false, false,
                false, false, Set.of(), Set.of());
        try {
            SourceSet javaBase = null; // FIXME
            DiagnosticCollector<JavaFileObject> diagnostics = ignoreErrors ? null : new DiagnosticCollector<>();
            javacTask = createTask(sourcesByClassName, jars, diagnostics);
            JavaInspector.ParseOptions parseOptions = new JavaInspectorImpl.ParseOptionsBuilder()
                    .setDetailedSources(true)
                    .build();
            ScanCompilationUnits scanCompilationUnits = new ScanCompilationUnits(runtime, javaBase,
                    javacTask, sourceSet, parseOptions, diagnostics);
            classSymbolScanner = scanCompilationUnits.classSymbolScanner();
            return scanCompilationUnits.scan();
        } catch (IOException io) {
            fail(io);
            return null;
        }
    }

    public void loadType(TypeInfo typeInfo) {
        Elements elements = javacTask.getElements();
        TypeElement typeElement = elements.getTypeElement(typeInfo.fullyQualifiedName());
        classSymbolScanner.loadType((Symbol.ClassSymbol) typeElement, typeInfo, true);
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
                List.of("-proc:none", "--enable-preview", "--release=25"),
                null,
                compilationUnits
        );
    }

    public String print2(CompilationUnit compilationUnit) {
        OutputBuilder ob = runtime.newCompilationUnitPrinter(compilationUnit, true)
                .print(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());
        Formatter formatter = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().build());
        return formatter.write(ob);
    }
}
