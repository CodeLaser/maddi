package org.e2immu.language.inspection.integration.java.translate;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
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
        TypeInfo clazz = javaInspector.compiledTypesManager().getOrLoad(Class.class);
        TypeInfo typeDescriptor = javaInspector.compiledTypesManager().getOrLoad(TypeDescriptor.class);
        TypeInfo typeDescriptorOfField = typeDescriptor.findSubType("OfField");
        assertTrue(clazz.interfacesImplemented().stream()
                .anyMatch(ii -> ii.typeInfo() == typeDescriptorOfField));
        assertTrue(typeDescriptorOfField.interfacesImplemented().stream()
                .anyMatch(ii -> ii.typeInfo() == typeDescriptor));
    }
}
