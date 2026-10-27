/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.inspection.integration.java.translate;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.lang.invoke.TypeDescriptor;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestTranslate extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import org.springframework.core.convert.ConversionService;
            import java.util.HashMap;
            import java.util.Map;
            
            import static org.assertj.core.api.Assertions.assertThat;
            
            class X {
                // know that: Class implements TypeDescriptor.OfField which extends java.lang.invoke.TypeDescriptor
                // boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType);
                // Object convert(@Nullable Object source, TypeDescriptor targetType);
                void scalarMapNotGenericTarget(ConversionService conversionService) {
                    Map<String, String> map = new HashMap<>();
                    map.put("1", "9");
                    map.put("2", "37");
            
                    assertThat(conversionService.canConvert(Map.class, Map.class)).isTrue();
                    assertThat((Map<?, ?>) conversionService.convert(map, Map.class)).isSameAs(map);
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        TypeInfo clazz = javaInspector.compiledTypesManager().type(Class.class);
        TypeInfo typeDescriptor = javaInspector.compiledTypesManager().type(TypeDescriptor.class);
        TypeInfo typeDescriptorOfField = typeDescriptor.findSubType("OfField");
        assertTrue(clazz.interfacesImplemented().stream()
                .anyMatch(ii -> ii.typeInfo() == typeDescriptorOfField));
        assertTrue(typeDescriptorOfField.interfacesImplemented().stream()
                .anyMatch(ii -> ii.typeInfo() == typeDescriptor));
    }
}
