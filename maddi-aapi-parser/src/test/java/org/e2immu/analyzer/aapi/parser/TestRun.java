/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.analyzer.aapi.parser;

import ch.qos.logback.classic.Level;
import org.e2immu.analyzer.modification.common.defaults.DebugVisitor;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
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
    private static final String JDK_HOMEBREW_21 = "jdk-Homebrew-21.0.9";
    private static final String JDK_HOMEBREW_24 = "jdk-Homebrew-24.0.2";
    private static final String JDK_HOMEBREW_25 = "jdk-Homebrew-25.0.1";

    public static final String LIBS_MADDI_HOMEBREW_25 = "libs.e2immu-Homebrew-25.0.1";
    public static final String LIBS_LOG_HOMEBREW_25 = "libs.log-Homebrew-25.0.1";
    public static final String LIBS_TEST_HOMEBREW_25 = "libs.test-Homebrew-25.0.1";
    public static final String LIBS_MADDI_ORACLE_24 = "libs.e2immu-OracleCorporation-24.0.2";
    public static final String LIBS_LOG_ORACLE_24 = "libs.log-OracleCorporation-24.0.2";
    public static final String LIBS_TEST_ORACLE_24 = "libs.test-OracleCorporation-24.0.2";

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
                    case JDK_HOMEBREW_24, JDK_HOMEBREW_21, JDK_HOMEBREW_25 -> 216;
                    case LIBS_MADDI_HOMEBREW_25, LIBS_MADDI_ORACLE_24 -> 0;
                    case LIBS_LOG_HOMEBREW_25, LIBS_LOG_ORACLE_24 -> 3;
                    case LIBS_TEST_HOMEBREW_25, LIBS_TEST_ORACLE_24 -> 4;
                    default -> -1;
                };
                assertEquals(expect, writeOut.size(), context());

                int expectSub = switch (context()) {
                    case JDK_HOMEBREW_24, JDK_HOMEBREW_21, JDK_HOMEBREW_25 -> 50;
                    case LIBS_MADDI_HOMEBREW_25, LIBS_LOG_HOMEBREW_25, LIBS_MADDI_ORACLE_24, LIBS_LOG_ORACLE_24 -> 0;
                    case LIBS_TEST_HOMEBREW_25, LIBS_TEST_ORACLE_24 -> 1;
                    default -> -1;
                };
                int subTypes = (int) writeOut.stream()
                        .filter(t -> t.compilationUnitOrEnclosingType().isRight()).count();
                assertEquals(expectSub, subTypes, context());
            }

            @Override
            public void afterAnnotatedApiParsing(JavaInspector javaInspector) {
                int expectCt = switch (context()) {
                    case JDK_HOMEBREW_24 -> 3208;
                    case JDK_HOMEBREW_21 -> 3558;
                    case JDK_HOMEBREW_25 -> 3600;
                    case LIBS_MADDI_HOMEBREW_25, LIBS_MADDI_ORACLE_24 -> 594;
                    case LIBS_LOG_HOMEBREW_25, LIBS_LOG_ORACLE_24 -> 609;
                    case LIBS_TEST_HOMEBREW_25 -> 619;
                    case LIBS_TEST_ORACLE_24 -> 615;
                    default -> -1;
                };
                assertEquals(expectCt, javaInspector.compiledTypesManager().typesLoaded(true).size(), context());

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
                int typesWithParentNonNullNonJLO = (int) javaInspector.compiledTypesManager().typesLoaded(true)
                        .stream().flatMap(TypeInfo::recursiveSubTypeStream)
                        .filter(ti -> ti.parentClass() != null && !ti.parentClass().isJavaLangObject())
                        .count();
                int expectParentNonNull = switch (context()) {
                    case JDK_HOMEBREW_24 -> 2746;
                    case JDK_HOMEBREW_21 -> 2926;
                    case JDK_HOMEBREW_25 -> 2982;
                    case LIBS_MADDI_HOMEBREW_25, LIBS_MADDI_ORACLE_24 -> 524;
                    case LIBS_LOG_HOMEBREW_25 -> 525;
                    case LIBS_LOG_ORACLE_24 -> 521;
                    case LIBS_TEST_HOMEBREW_25, LIBS_TEST_ORACLE_24 -> 529;
                    default -> -1;
                };
                assertEquals(expectParentNonNull, typesWithParentNonNullNonJLO, context());

                int expectSf = switch (context()) {
                    case JDK_HOMEBREW_24, JDK_HOMEBREW_21, JDK_HOMEBREW_25 -> 25;
                    case LIBS_MADDI_HOMEBREW_25, LIBS_MADDI_ORACLE_24 -> 0;
                    case LIBS_LOG_HOMEBREW_25, LIBS_LOG_ORACLE_24 -> 1;
                    case LIBS_TEST_HOMEBREW_25, LIBS_TEST_ORACLE_24 -> 2;
                    default -> -1;
                };
                assertEquals(expectSf, javaInspector.sourceFiles().size(), context());

                int stmSize = switch (context()) {
                    case JDK_HOMEBREW_24, JDK_HOMEBREW_21, JDK_HOMEBREW_25 -> 241;
                    case LIBS_MADDI_HOMEBREW_25, LIBS_MADDI_ORACLE_24 -> 0;
                    case LIBS_LOG_HOMEBREW_25, LIBS_LOG_ORACLE_24 -> 4;
                    case LIBS_TEST_HOMEBREW_25, LIBS_TEST_ORACLE_24 -> 6;
                    default -> -1;
                };
                int sizeFiltered = javaInspector.compiledTypesManager().typesLoaded(false).size();
                assertEquals(stmSize, sizeFiltered, context());
            }

            @Override
            public DebugVisitor debugVisitor() {
                return new DebugVisitor() {
                    @Override
                    public void inputTypes(List<TypeInfo> allTypes) {
                        int expect = switch (context()) {
                            case JDK_HOMEBREW_24, JDK_HOMEBREW_21, JDK_HOMEBREW_25 -> 216;
                            case LIBS_MADDI_HOMEBREW_25, LIBS_MADDI_ORACLE_24 -> 0;
                            case LIBS_LOG_HOMEBREW_25, LIBS_LOG_ORACLE_24 -> 3;
                            case LIBS_TEST_HOMEBREW_25, LIBS_TEST_ORACLE_24 -> 4;
                            default -> -1;
                        };
                        assertEquals(expect, allTypes.size(), context());
                    }

                    @Override
                    public void allTypes(List<TypeInfo> allTypes) {
                        int expect = switch (context()) {
                            case JDK_HOMEBREW_21 -> 297;// 235;
                            case JDK_HOMEBREW_24 -> 297;// 237;//277;// 235;
                            case LIBS_MADDI_HOMEBREW_25, LIBS_MADDI_ORACLE_24 -> 0;
                            case LIBS_LOG_HOMEBREW_25, LIBS_LOG_ORACLE_24 -> 4;
                            case LIBS_TEST_HOMEBREW_25, LIBS_TEST_ORACLE_24 -> 5;
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
