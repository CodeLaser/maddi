package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestAssignmentsReuse extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.io.*;
            import java.net.HttpURLConnection;
            import java.net.URL;
            class X {
                public static String connect(String url_str, String oauth_header, String data) throws IOException {
                    URL url = new URL(url_str);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setAllowUserInteraction(true);
                    if (oauth_header != null || data != null) {
                        if (data != null) {
                            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                            out.writeBytes(data);
                            out.flush();
                            out.close();
                        }
                    }
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String out = "";
                    String temp;
                    while ((temp = reader.readLine()) != null) {
                        out += temp;
                    }
                    return out;
                }
            }
            """;

    @DisplayName("variable is used 2x, with different types")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        MethodInfo method = X.findUniqueMethod("connect", 3);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        LocalVariableCreation reader = (LocalVariableCreation) method.methodBody().statements().get(4);
        Statement if300 = method.methodBody().statements().get(3).block().statements().get(0);
        VariableData vd300 = VariableDataImpl.of(if300);
        assertFalse(vd300.isKnown("out"));
        Statement mc30003 = if300.block().statements().get(3);
        VariableData vd30003 = VariableDataImpl.of(mc30003);
        VariableInfo vi1 = vd30003.variableInfo("out");
        assertEquals("Type java.io.DataOutputStream", vi1.variable().parameterizedType().toString());

        VariableData vd4 = VariableDataImpl.of(reader);
        assertFalse(vd4.isKnown("out"));
        VariableData vdMethod = VariableDataImpl.of(method);
        VariableInfo vi = vdMethod.variableInfo("out");
        assertEquals("Type String", vi.variable().parameterizedType().toString());
    }

}
