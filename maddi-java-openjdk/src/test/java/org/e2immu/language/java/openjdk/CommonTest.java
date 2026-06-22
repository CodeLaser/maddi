package org.e2immu.language.java.openjdk;

import ch.qos.logback.classic.Level;
import com.sun.source.util.JavacTask;
import lombok.Data;
import org.assertj.core.api.Assert;
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
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.e2immu.support.SetOnce;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.e2immu.language.inspection.resource.SourceSetImpl.sourceSetOf;
import static org.junit.jupiter.api.Assertions.fail;

public class CommonTest {

    protected final Runtime runtime;
    protected final List<String> preload;
    protected final InfoByFqn infoByFqn = new InfoByFqn();
    protected JavacTask javacTask;
    protected SourceSet sourceSet;
    protected ClassSymbolScanner classSymbolScanner;

    public CommonTest() {
        this(List.of());
    }

    public CommonTest(List<String> preload) {
        this.preload = preload;
        this.runtime = new RuntimeImpl();
    }

    public TypeInfo scan(String fqn, String content) {
        return scan(false, Map.of(fqn, content)).primaryTypes().getFirst();
    }

    public Map<String, TypeInfo> scan(boolean ignoreErrorss, String... fqnContentPairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < fqnContentPairs.length; i += 2) {
            map.put(fqnContentPairs[i], fqnContentPairs[i + 1]);
        }
        List<TypeInfo> typeInfoList = scan(ignoreErrorss, map).primaryTypes();
        return typeInfoList.stream().collect(Collectors.toUnmodifiableMap(Info::fullyQualifiedName, ti -> ti));
    }

    public ScanCompilationUnits.Result scan(boolean ignoreErrors, Map<String, String> sourcesByClassName) {
        sourceSet = new SourceSetImpl.Builder().setName("source").setUri(URI.create("file:/")).build();
        try {
            SourceSet javaBase = SourceSetImpl.javaBase();

            SourceSet javaNetHttp = new SourceSetImpl.Builder().setName("java.net.http").setUri(URI.create("file:/"))
                    .setLibrary(true)
                    .setExternalLibrary(true).setPartOfJdk(true).setModule(true).setDependencies(List.of(javaBase))
                    .build();

            SourceSet orgSlf4j = sourceSetOf(Logger.class, javaBase);
            SourceSet logBackClassic = sourceSetOf(Level.class);
            SourceSet logBackCore = sourceSetOf(ch.qos.logback.core.util.CloseUtil.class);
            SourceSet annotations = sourceSetOf(NotNull.class, javaBase);
            SourceSet maddiSupport = sourceSetOf(SetOnce.class, javaBase);
            SourceSet junitJupiter = sourceSetOf(Assertions.class, javaBase);
            SourceSet assertJ = sourceSetOf(Assert.class, javaBase);
            SourceSet lombok = sourceSetOf(Data.class, javaBase);

            MaddiDiagnosticCollector diagnostics = new MaddiDiagnosticCollector(ignoreErrors);
            javacTask = createTask(sourcesByClassName, List.of(), diagnostics);

            InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                    .addSourceSets(sourceSet)
                    .addClassPathParts(javaBase, javaNetHttp)
                    .addClassPathParts(orgSlf4j, logBackClassic, logBackCore,
                            annotations, maddiSupport, junitJupiter, assertJ, lombok)
                    .build();
            ScanCompilationUnits scanCompilationUnits = new ScanCompilationUnits(runtime, inputConfiguration,
                    javacTask, sourceSet, infoByFqn, true, diagnostics,
                    preload);
            classSymbolScanner = scanCompilationUnits.classSymbolScanner();
            return scanCompilationUnits.scan();
        } catch (IOException | URISyntaxException io) {
            fail(io);
            return null;
        }
    }

    private JavacTask createTask(Map<String, String> sourcesByClassName,
                                 List<File> jars,
                                 MaddiDiagnosticCollector diagnostics) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, null, null);

        if (!jars.isEmpty()) {
            fm.setLocation(StandardLocation.CLASS_PATH, jars);
        }

        // Wrap each source string in an InMemoryJavaFileObject
        List<JavaFileObject> compilationUnits = sourcesByClassName.entrySet().stream()
                .map(e -> new InMemoryJavaFileObject("source", e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return (JavacTask) compiler.getTask(
                null, fm, diagnostics,
                List.of("-processor", "lombok.launch.AnnotationProcessorHider$AnnotationProcessor",
                        "--enable-preview", "--release=26"),
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
