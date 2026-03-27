package org.e2immu.language.inspection.integration.java.print;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
public class TestVariousPrint2Issues extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            public class ExceptionHolder_t {
                private Exception exception;
                private void getExceptionStackBody() {
                    System.out.println("this is a prettly long line");
                    getException().printStackTrace(null);
                }
                public Exception getException() { return this.exception; }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        String reprint = javaInspector.print2(X.compilationUnit());
        assertEquals(INPUT1, reprint);
    }

    @Language("java")
    private static final String INPUT2 = """
            import java.nio.charset.Charset;
            import java.nio.charset.StandardCharsets;
            class ConfigUtil {
                private static Charset getCharset() {
                    /* one */
                    System.out.println("*");
            
                    /* one */
                    // followed by another
                    System.out.println("#");
            
                    /* one
                       on two lines
                     */
                    // followed by another
                    System.out.println("#");
            
                    // one line
                    System.out.println("?");
            
                    // one
                    // two
                    // three
                    System.out.println("!");
            
                    // SentinelConfig
                    // so not use SentinelConfig.charset()
                    return Charset.forName(System.getProperty("csp.sentinel.charset",StandardCharsets.UTF_8.name()));
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        String reprint = javaInspector.print2(X.compilationUnit());
        assertEquals(INPUT2, reprint);
    }

    @Language("java")
    private static final String INPUT3 = """
            import java.io.File;
            import java.io.FileWriter;
            import java.io.IOException;
            import static java.lang.String.format;
            
            class AgentLauncher {
                private void method(File file) {
                    try (final FileWriter fw = new FileWriter(file, true)) {
                        fw.append(format("%s",file.getName()));
                        fw.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo X = javaInspector.parse(INPUT3);
        String reprint = javaInspector.print2(X.compilationUnit());
        assertEquals(INPUT3, reprint);
    }
}
