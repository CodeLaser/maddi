package org.e2immu.analyzer.run.openjdkmain;

import ch.qos.logback.classic.Level;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.run.openjdkmain.javac.ParseJavacList;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.openjdk.JavaInspectorImpl;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/*
 Faithful linking benchmark on the real io.codelaser...parseq code (deep recursive generic structures that the
 shared-variable engine is meant to bound). Reads a gradle --debug compile log of codelaser-stdbase-util (which
 carries the javac 'Compiler arguments:' lines), builds an InputConfiguration via ParseJavacList, parses, and
 times PrepAnalyzer + LinkComputer on the parseq package. Skipped unless the log is present.

 To (re)capture the log:
   cd ~/git/jfocus-stdbase && ./gradlew :codelaser-stdbase-util:compileJava --rerun-tasks --debug --console=plain \\
       > /tmp/jfocus-debug.log 2>&1
 Override the path with -Djfocus.log=/path/to/log
 */
public class TestParSeqLinkBench {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestParSeqLinkBench.class);
    private static final String PARSEQ_PKG = "io.codelaser.jfocus.stdbase.util.parseq";

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
    }

    @Test
    public void bench() throws Exception {
        Path log = Path.of(System.getProperty("jfocus.log", "/tmp/jfocus-test-debug.log"));
        Assumptions.assumeTrue(Files.exists(log), "capture the jfocus compile log first (see class comment): " + log);

        InputConfiguration inputConfiguration = new ParseJavacList().parse(log);
        JavaInspector javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.initialize(inputConfiguration);

        JavaInspector.ParseOptions parseOptions = new JavaInspector.ParseOptions.Builder()
                .setDetailedSources(true).setFailFast(false).setParallel(false).setIgnoreModule(true).build();
        long t0 = System.nanoTime();
        Summary summary = javaInspector.parse(parseOptions);
        long t1 = System.nanoTime();
        ParseResult parseResult = summary.parseResult();

        List<TypeInfo> parseqTypes = parseResult.primaryTypes().stream()
                .filter(t -> PARSEQ_PKG.equals(t.packageName())).toList();
        LOGGER.info("Parsed {} primary types, {} in parseq; errors={}",
                parseResult.primaryTypes().size(), parseqTypes.size(), summary.haveErrors());
        Assumptions.assumeFalse(parseqTypes.isEmpty(), "no parseq types parsed");

        PrepAnalyzer prep = new PrepAnalyzer(javaInspector.runtime());
        prep.doPrimaryTypesReturnComputeCallGraph(Set.copyOf(parseResult.primaryTypes()),
                parseResult.sourceSetToModuleInfoMap().values(), _ -> false, false);
        long t2 = System.nanoTime();

        // PRODUCTION disables the checkDuplicateNames debug assertion so linking runs to completion (we are
        // measuring time on deep structures, not asserting graph invariants).
        LinkComputerImpl link = new LinkComputerImpl(javaInspector, org.e2immu.analyzer.modification.link.LinkComputer.Options.PRODUCTION);
        // first link the production parseq types so their method summaries (callees of the tests) are available
        for (TypeInfo t : parseqTypes) {
            if (t.simpleName().startsWith("Test") || t.simpleName().startsWith("Common")) continue;
            link.doPrimaryType(t);
        }
        long t3 = System.nanoTime();
        LOGGER.info("PARSEQ BENCH  parse={}ms  prep={}ms  LINK(production parseq types)={}ms",
                (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000, (t3 - t2) / 1_000_000);

        // now the target: time each test method of TestParSeqElement individually
        TypeInfo test = parseqTypes.stream().filter(t -> "TestParSeqElement".equals(t.simpleName()))
                .findFirst().orElseThrow(() -> new AssertionError("TestParSeqElement not parsed (use the test compile log)"));
        long testTotal = 0;
        for (var m : test.methodStream().filter(mi -> !mi.isSynthetic()).toList()) {
            long s = System.nanoTime();
            link.doMethod(m);
            long ms = (System.nanoTime() - s) / 1_000_000;
            testTotal += ms;
            LOGGER.info("  TEST-METHOD {} : {} ms", m.name(), ms);
        }
        LOGGER.info("TESTPARSEQELEMENT total methods = {} ms", testTotal);
    }
}
