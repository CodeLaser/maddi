package io.codelaser.maddi.modification.common;

import io.codelaser.maddi.modification.common.util.IsolateMethod;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two JDK types with the same simple name, one named simply in the pasted text and one reached only through a
 * reconstructed signature. Only one of them can own the simple name in the frame, and the pasted text is verbatim:
 * its spelling is fixed, so it is the one that has to win.
 * <p>
 * The concrete case is {@code java.sql.Date} vs {@code java.util.Date} — closed-core's
 * {@code ExportJob.insertRecords}. With four or more {@code java.sql} types collected the import
 * computer collapses them to {@code import java.sql.*}, an on-demand import; a single-type
 * {@code import java.util.Date} then beats it, the verbatim {@code Date dateValue} silently means
 * {@code java.util.Date}, and {@code PreparedStatement.setDate(int, java.sql.Date)} rejects it.
 */
public class TestIsolateMethod17ImportCollision extends CommonIsolateMethodTest {

    @Override
    @BeforeEach
    public void beforeEach() throws IOException {
        // java.sql is not on the lean test class path
        javaInspector = CommonTest.javaInspectorFactory("java.sql")
                .withSources(SourceSetImpl.testProtocolSourceSet());
        isolateMethod = new IsolateMethod(javaInspector, "");
    }

    @Language("java")
    public static final String HELPER = """
            package a.b;
            public class Helper {
                public java.util.Date when() { return null; }
            }
            """;

    @Language("java")
    public static final String X = """
            package a.b;
            import java.sql.Connection;
            import java.sql.Date;
            import java.sql.PreparedStatement;
            import java.sql.ResultSet;
            import java.sql.SQLException;
            public class X {
                void method(Connection connection, Helper helper) throws SQLException {
                    PreparedStatement pstmt = connection.prepareStatement("select 1");
                    Date dateValue = new Date(helper.when().getTime());
                    pstmt.setDate(1, dateValue);
                    ResultSet rs = pstmt.executeQuery();
                    rs.close();
                }
            }
            """;

    @DisplayName("java.sql.Date is written simply, java.util.Date only reconstructed: the written one keeps the name")
    @Test
    public void dateCollision() {
        TypeInfo x = javaInspector.parse(Map.of("a.b.Helper", HELPER, "a.b.X", X),
                        new JavaInspector.ParseOptions.Builder().setDetailedSources(true).setFailFast(true).build())
                .parseResult().findType("a.b.X");
        String m = """
                void method(Connection connection, Helper helper) throws SQLException {
                    PreparedStatement pstmt = connection.prepareStatement("select 1");
                    Date dateValue = new Date(helper.when().getTime());
                    pstmt.setDate(1, dateValue);
                    ResultSet rs = pstmt.executeQuery();
                    rs.close();
                }""";
        String out = isolate(x, "method", 2, m);
        System.out.println(out);
        // 'Date' in the pasted text is java.sql.Date; java.util.Date, reached only through Helper.when(), must not
        // take the slot with a single-type import
        assertFalse(out.contains("import java.util.Date;"));
        assertTrue(out.contains("java.util.Date when()"));
        javaInspector.invalidateAllSources();
        assertNotNull(javaInspector.parse("X_method", out));
    }
}
