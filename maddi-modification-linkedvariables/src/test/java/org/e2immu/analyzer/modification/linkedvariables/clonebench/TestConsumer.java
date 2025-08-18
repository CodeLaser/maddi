package org.e2immu.analyzer.modification.linkedvariables.clonebench;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;


public class TestConsumer extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.io.File;
            import java.io.FileReader;
            import java.io.IOException;
            import java.util.Collections;
            import java.util.Objects;
            import java.util.Properties;
            import java.util.function.Predicate;
            
            public class PropertyDump {
              public static void main(String[] args) {
                Predicate<String> nameSelectionPredicate =
                    (name) -> name.startsWith("test.") || name.startsWith("jetty.");
                // As System Properties
                Properties props = System.getProperties();
                props.stringPropertyNames().stream()
                    .filter(nameSelectionPredicate)
                    .sorted()
                    .forEach((name) -> System.out.printf("System %s=%s%n", name, props.getProperty(name)));
                // As File Argument
                for (String arg : args) {
                  if (arg.endsWith(".properties")) {
                    Properties aprops = new Properties();
                    File propFile = new File(arg);
                    try (FileReader reader = new FileReader(propFile)) {
                      aprops.load(reader);
                      Collections.list(aprops.propertyNames()).stream()
                          .map(Objects::toString)
                          .filter(nameSelectionPredicate)
                          .sorted()
                          .forEach((name) -> System.out.printf(
                                      "%s %s=%s%n", propFile.getName(), name, aprops.getProperty(name)));
                    } catch (IOException e) {
                      e.printStackTrace();
                    }
                  }
                }
              }
            }
            """;

    @DisplayName("nulls in HiddenContentSelector and LinkHelper")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }

}
