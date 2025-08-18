package org.e2immu.analyzer.modification.linkedvariables.link;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestFollow extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            import java.io.File;
            import java.io.FileFilter;
            import java.util.ArrayList;
            import java.util.Arrays;
            import java.util.Collections;
            import java.util.List;
            
            public class B {
                public static File[] listDirs(File pathname) {
                    return pathname.listFiles(new FileFilter() {
            
                        public boolean accept(File file) {
                            return file.isDirectory() && !file.isHidden();
                        }
                    });
                }

                public static List<File> listDirsSubdirs(File pathname) {
                    if (!pathname.exists() || !pathname.isDirectory()) {
                        return Collections.EMPTY_LIST;
                    }
                    List<File> dirsSubdirs = new ArrayList<File>(50);
                    List<File> curLoop = new ArrayList<File>(20);
                    List<File> nextLoop = new ArrayList<File>(20);
                    curLoop.addAll(Arrays.asList(listDirs(pathname)));
                    while (!curLoop.isEmpty()) {
                        for (File dir : curLoop) {
                            nextLoop.addAll(Arrays.asList(listDirs(dir)));
                        }
                        dirsSubdirs.addAll(curLoop);
                        curLoop.clear();
                        curLoop.addAll(nextLoop);
                        nextLoop.clear();
                    }
                    Collections.sort(dirsSubdirs);
                    return dirsSubdirs;
                }
            }
            """;

    @DisplayName("issue in LinkHelper.follow")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }
}
