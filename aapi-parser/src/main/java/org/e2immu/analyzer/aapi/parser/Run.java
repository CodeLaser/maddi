package org.e2immu.analyzer.aapi.parser;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.analyzer.modification.common.defaults.ShallowAnalyzer;
import org.e2immu.analyzer.modification.io.WriteAnalysis;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.ToolChain;
import org.e2immu.util.internal.util.Trie;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Run {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Run.class);

    public static final String AAPI_DIR =
            "../e2immu-aapi-archive/src/main/java/org/e2immu/analyzer/aapi/archive/v2";
    public static final String APF_DIR =
            "../e2immu-aapi-archive/src/main/resources/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles";

    public static void main(String[] args) throws IOException {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("org.e2immu.analyzer.shallow")).setLevel(Level.DEBUG);
        ((Logger) LoggerFactory.getLogger("org.e2immu.analyzer.modification.prepwork")).setLevel(Level.DEBUG);
        new Run().go();
    }

    private final RunVisitor runVisitor;

    public Run() {
        this(null);
    }

    public Run(RunVisitor runVisitor) {
        this.runVisitor = runVisitor;
    }

    public List<Message> go() throws IOException {
        List<Message> messages = new ArrayList<>();
        for (ToolChain.JRE jre : ToolChain.JRES) {
            if ("Homebrew".equals(jre.vendor()) && 21 <= jre.mainVersion()) {
                messages.addAll(go("jdk", "jdk/openjdk-" + jre.platformVersion(), jre));
            }
        }
        ToolChain.JRE jre = ToolChain.currentJre();
        for (String lib : new String[]{"e2immu", "log", "test"}) {
            messages.addAll(go("libs." + lib, "libs/" + lib, jre));
        }
        return messages;
    }

    public List<Message> go(String subDirIn, String subDirOut, ToolChain.JRE jre) throws IOException {
        LOGGER.info("I'm at {}; going for {}, {}", new File(".").getAbsolutePath(), subDirIn, jre.shortName());
        if (runVisitor != null) runVisitor.setContext(subDirIn, subDirOut, jre);

        AnnotatedApiParser annotatedApiParser = new AnnotatedApiParser();
        List<String> classPath = new ArrayList<>();
        Collections.addAll(classPath, ToolChain.CLASSPATH_JUNIT);
        Collections.addAll(classPath, ToolChain.CLASSPATH_SLF4J_LOGBACK);
        classPath.add(JavaInspectorImpl.E2IMMU_SUPPORT);
        annotatedApiParser.initialize(jre.path(),
                classPath,
                List.of(AAPI_DIR),
                List.of(subDirIn));
        if (runVisitor != null) {
            runVisitor.afterAnnotatedApiParsing(annotatedApiParser.javaInspector());
        }
        ShallowAnalyzer shallowAnalyzer = new ShallowAnalyzer(annotatedApiParser.runtime(), annotatedApiParser,
                true, runVisitor == null ? null : runVisitor.debugVisitor());
        ShallowAnalyzer.Result rs = shallowAnalyzer.go(annotatedApiParser.types());
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(annotatedApiParser.runtime());
        prepAnalyzer.initialize(annotatedApiParser.javaInspector().compiledTypesManager().typesLoaded());

        Set<Element> hasAnnotations = annotatedApiParser.infos();
        LOGGER.info("Parsed and analyzed {} types; {} info objects", annotatedApiParser.types().size(),
                hasAnnotations.size());
        hasAnnotations.forEach(ha -> {
            if (ha instanceof Info i && !i.analysis().haveAnalyzedValueFor(PropertyImpl.ANNOTATED_API)) {
                i.analysis().set(PropertyImpl.ANNOTATED_API, ValueImpl.BoolImpl.TRUE);
            }
        });

        Set<TypeInfo> writeOut = new HashSet<>(annotatedApiParser.types());
        if (runVisitor != null) runVisitor.writeAnalysis(writeOut);
        WriteAnalysis wa = new WriteAnalysis(annotatedApiParser.runtime(), writeOut::contains);
        Trie<TypeInfo> trie = new Trie<>();
        for (TypeInfo ti : annotatedApiParser.types()) {
            if (ti.isPrimaryType()) {
                trie.add(ti.packageName().split("\\."), ti);
            }
        }
        File dir = new File(APF_DIR);
        File subDirOutFile = new File(dir, subDirOut);
        wa.write(subDirOutFile.getAbsolutePath(), trie);

        WriteDecoratedAAPI writeDecoratedAAPI = new WriteDecoratedAAPI(annotatedApiParser.javaInspector(),
                annotatedApiParser::data, i -> rs.dataMap().get(i));
        File decorated = new File("build/decorated");
        File subDirDeco = new File(decorated, subDirOut);
        if (subDirDeco.mkdirs()) {
            LOGGER.info("Created directory {}", subDirDeco);
        }
        writeDecoratedAAPI.write(subDirDeco.getAbsolutePath(), trie, "org.e2immu");
        return rs.messages();
    }
}
