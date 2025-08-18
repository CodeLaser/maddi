package org.e2immu.analyzer.aapi.parser;

import ch.qos.logback.classic.Level;
import org.e2immu.analyzer.modification.common.defaults.DebugVisitor;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.impl.parser.SourceTypeMapImpl;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.ToolChain;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestRun {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestRun.class);

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.e2immu.analyzer.aapi")).setLevel(Level.DEBUG);
    }

    @Test
    public void test() throws IOException {
        Run run = new Run(new RunVisitor() {
            private String libIn;
            private ToolChain.JRE jre;

            @Override
            public void setContext(String libIn, String libOut, ToolChain.JRE jre) {
                this.libIn = libIn;
                this.jre = jre;
            }

            private String context() {
                return libIn + "-" + jre.shortName();
            }

            @Override
            public void writeAnalysis(Set<TypeInfo> writeOut) {
                int expect = switch (context()) {
                    case "jdk-Homebrew-24.0.1", "jdk-Homebrew-21.0.7" -> 216;
                    case "libs.e2immu-Homebrew-24.0.1" -> 0;
                    case "libs.log-Homebrew-24.0.1" -> 3;
                    case "libs.test-Homebrew-24.0.1" -> 4;
                    default -> -1;
                };
                assertEquals(expect, writeOut.size(), context());

                int expectSub = switch (context()) {
                    case "jdk-Homebrew-24.0.1", "jdk-Homebrew-21.0.7" -> 50;
                    case "libs.e2immu-Homebrew-24.0.1", "libs.log-Homebrew-24.0.1" -> 0;
                    case "libs.test-Homebrew-24.0.1" -> 1;
                    default -> -1;
                };
                int subTypes = (int) writeOut.stream()
                        .filter(t -> t.compilationUnitOrEnclosingType().isRight()).count();
                assertEquals(expectSub, subTypes, context());
            }

            @Override
            public void afterAnnotatedApiParsing(JavaInspector javaInspector) {
                int expectCt = switch (context()) {
                    case "jdk-Homebrew-24.0.1" -> 3208;
                    case "jdk-Homebrew-21.0.7" -> 3210;
                    case "libs.e2immu-Homebrew-24.0.1" -> 448;
                    case "libs.log-Homebrew-24.0.1" -> 457;
                    case "libs.test-Homebrew-24.0.1" -> 465;
                    default -> -1;
                };
                assertEquals(expectCt, javaInspector.compiledTypesManager().typesLoaded().size(), context());

                /*
                IMPORTANT: as soon as this query runs, the values for 'parent' change
                this is dependent on parsing order (hash maps), dynamic loading in the compiled types manager
                some types are only partially loaded, when they appear as types in fields, parameters of methods)
                
                int typesWithEnclosing = (int) javaInspector.compiledTypesManager().typesLoaded()
                        .stream().flatMap(TypeInfo::recursiveSubTypeStream)
                        .filter(ti -> ti.compilationUnitOrEnclosingType().isRight())
                        .count();
                int expectEnclosing = switch (context()) {
                    case "jdk-Homebrew-24.0.1" -> 3456;
                    case "jdk-Homebrew-21.0.7" -> 2736;
                    case "libs.e2immu-Homebrew-24.0.1" -> 286;
                    case "libs.log-Homebrew-24.0.1" -> 287;
                    case "libs.test-Homebrew-24.0.1" -> 288;
                    default -> -1;
                };
                assertEquals(expectEnclosing, typesWithEnclosing, context());
                */
                int typesWithParentNonNullNonJLO = (int) javaInspector.compiledTypesManager().typesLoaded()
                        .stream().flatMap(TypeInfo::recursiveSubTypeStream)
                        .filter(ti -> ti.parentClass() != null && !ti.parentClass().isJavaLangObject())
                        .count();
                int expectParentNonNull = switch (context()) {
                    case "jdk-Homebrew-24.0.1" -> 2746;
                    case "jdk-Homebrew-21.0.7" -> 2736;
                    case "libs.e2immu-Homebrew-24.0.1" -> 286;
                    case "libs.log-Homebrew-24.0.1" -> 287;
                    case "libs.test-Homebrew-24.0.1" -> 288;
                    default -> -1;
                };
                assertEquals(expectParentNonNull, typesWithParentNonNullNonJLO, context());

                int expectSf = switch (context()) {
                    case "jdk-Homebrew-24.0.1", "jdk-Homebrew-21.0.7" -> 25;
                    case "libs.e2immu-Homebrew-24.0.1" -> 0;
                    case "libs.log-Homebrew-24.0.1" -> 1;
                    case "libs.test-Homebrew-24.0.1" -> 2;
                    default -> -1;
                };
                assertEquals(expectSf, javaInspector.sourceFiles().size(), context());
                SourceTypeMapImpl sourceTypeMap = ((JavaInspectorImpl) javaInspector).getSourceTypeMap();
                int stmSize = switch (context()) {
                    case "jdk-Homebrew-24.0.1", "jdk-Homebrew-21.0.7" -> 241;
                    case "libs.e2immu-Homebrew-24.0.1" -> 0;
                    case "libs.log-Homebrew-24.0.1" -> 4;
                    case "libs.test-Homebrew-24.0.1" -> 6;
                    default -> -1;
                };
                assertEquals(stmSize, sourceTypeMap.getMap().size(), context());
            }

            @Override
            public DebugVisitor debugVisitor() {
                return new DebugVisitor() {
                    @Override
                    public void inputTypes(List<TypeInfo> allTypes) {
                        int expect = switch (context()) {
                            case "jdk-Homebrew-24.0.1", "jdk-Homebrew-21.0.7" -> 216;
                            case "libs.e2immu-Homebrew-24.0.1" -> 0;
                            case "libs.log-Homebrew-24.0.1" -> 3;
                            case "libs.test-Homebrew-24.0.1" -> 4;
                            default -> -1;
                        };
                        assertEquals(expect, allTypes.size(), context());
                    }

                    @Override
                    public void allTypes(List<TypeInfo> allTypes) {
                        int expect = switch (context()) {
                            case "jdk-Homebrew-21.0.7" -> 297;// 235;
                            case "jdk-Homebrew-24.0.1" -> 297;// 237;//277;// 235;
                            case "libs.e2immu-Homebrew-24.0.1" -> 0;
                            case "libs.log-Homebrew-24.0.1" -> 4;
                            case "libs.test-Homebrew-24.0.1" -> 5;
                            default -> -1;
                        };
                        //assertEquals(expect, allTypes.size(), context());
                    }
                };
            }
        });
        List<Message> messages = run.go();
        LOGGER.info("Have {} message(s)", messages.size());
        messages.forEach(m -> {
            LOGGER.info("{} {}: {}", m.level(), m.info(), m.message());
        });
    }
}
