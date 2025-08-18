package org.e2immu.analyzer.modification.linkedvariables.clonebench;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;


public class TestNullInHCSFindAll extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            //onlyjava
            import java.io.File;
            import java.util.Vector;
            
            public class Function1152888_file1952792 {
            
                public File[] makeClassPathArray() {
                    String classPath;
                    classPath = System.getProperty("java.boot.class.path");
                    int instanceOfSep = -1;
                    int nextInstance = classPath.indexOf(File.pathSeparatorChar, instanceOfSep + 1);
                    Vector elms = new Vector();
                    while (nextInstance != -1) {
                        elms.add(new File(classPath.substring(instanceOfSep + 1, nextInstance)));
                        instanceOfSep = nextInstance;
                        nextInstance = classPath.indexOf(File.pathSeparatorChar, instanceOfSep + 1);
                    }
                    elms.add(new File(classPath.substring(instanceOfSep + 1)));
                    File[] result = new File[elms.size()];
                    elms.copyInto(result);
                    return result;
                }
            }
            """;

    @DisplayName("@Identity method")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }

}
