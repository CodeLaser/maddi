package org.e2immu.analyzer.modification.linkedvariables.modification;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestModifiedParameter extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.io.File;
            import java.util.List;
            
            class Function18024_file101780 {
                private int findSources(File dir, List<String> args) {
                    File[] files = dir.listFiles();
                    if (files == null || files.length == 0) return 0;
                    int found = 0;
                    for (int i = 0; i < files.length; i++) {
                        File file = files[i];
                        if (file.isDirectory()) {
                            found += findSources(file, args);
                        } else if (file.getName().endsWith(".java")) {
                            args.add(file.toString());
                            found++;
                        }
                    }
                    return found;
                }
            }
            """;

    @DisplayName("used to fix a null-pointer exception")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        MethodInfo findSources = B.findUniqueMethod("findSources", 2);
        ParameterInfo p1 = findSources.parameters().get(1);
        assertTrue(p1.isModified());
    }
}
