package org.e2immu.analyzer.modification.linkedvariables.clonebench;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestTypeParameterChoices extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.io.FileInputStream;
            import java.io.FileNotFoundException;
            import java.io.IOException;
            import java.util.Properties;
            
            public class Function2210693_file1024232 {
            
                public static void loadProperties() {
                    Properties props = new Properties();
                    try {
                        props.load(new FileInputStream("src/properties"));
                        System.getProperties().putAll(props);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            """;

    @DisplayName("Properties is a HashTable<Object, Object>")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        TypeInfo properties = javaInspector.compiledTypesManager().get(Properties.class);
        assertEquals("0=Object", properties.analysis().getOrNull(HiddenContentTypes.HIDDEN_CONTENT_TYPES,
                HiddenContentTypes.class).detailedSortedTypes());
    }
}
