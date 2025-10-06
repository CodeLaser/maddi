package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestMethodCall13 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
         package a.b;
         import java.util.List;
         import java.util.Map;import java.util.stream.Collectors;
         
         class X {
             interface ClinicalDataCount { 
                 boolean accept();
             }
             interface ClinicalDataCountItem {
                  List<ClinicalDataCount> getCounts();
                  void setCounts(List<ClinicalDataCount> list);
                  String getAttributeId();
              }

             public Map<String, ClinicalDataCountItem> method(List<ClinicalDataCountItem> clinicalDataCountItems) {
                 return clinicalDataCountItems.stream()
                       // Exclude NA category
                       .map(
                           clinicalDataCountItem -> {
                             List<ClinicalDataCount> filteredClinicalDataCount =
                                 clinicalDataCountItem.getCounts().stream()
                                     .filter(
                                         clinicalDataCount -> {
                                           if(clinicalDataCount.accept()) {
                                               return true;
                                           }
                                           return false;
                                         })
                                     .collect(Collectors.toList());
                             clinicalDataCountItem.setCounts(filteredClinicalDataCount);
                             return clinicalDataCountItem;
                           })
                       .collect(
                           Collectors.toMap(
                               clinicalDataCountItem -> clinicalDataCountItem.getAttributeId(),
                               clinicalDataCountItem -> clinicalDataCountItem));
                }
            }
            """;

    /*
    public static <T, R> Collector<T, R, R> of(Supplier<R> supplier,
                                               BiConsumer<R, T> accumulator,
                                               BinaryOperator<R> combiner);
    R clearly can become a LinkedHashSet
    evaluation of the lambda must occur with R=LinkedHashSet, as must evaluation of X::combiner
     */
    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo method = typeInfo.findUniqueMethod("method", 1);
        Expression expression = method.methodBody().statements().getFirst().expression();
        assertEquals("", expression.toString());
    }

}