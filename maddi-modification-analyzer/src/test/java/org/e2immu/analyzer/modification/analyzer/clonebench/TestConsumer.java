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

package org.e2immu.analyzer.modification.analyzer.clonebench;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
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
