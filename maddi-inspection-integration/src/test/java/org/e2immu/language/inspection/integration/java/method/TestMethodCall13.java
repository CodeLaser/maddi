package org.e2immu.language.inspection.integration.java.method;

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

 //   @Language("java")
    private static final String INPUT1 = """
         return clinicalDataCountItems.stream()
               // Exclude NA category
               .map(
                   clinicalDataCountItem -> {
                     List<ClinicalDataCount> filteredClinicalDataCount =
                         clinicalDataCountItem.getCounts().stream()
                             .filter(
                                 clinicalDataCount -> {
                                   if (ComparisonCategoricalNaValuesString != null) {
                                     String[] ComparisonCategoricalNaValues =
                                         ComparisonCategoricalNaValuesString.split("\\\\|");
                                     for (String naValue : ComparisonCategoricalNaValues) {
                                       if (clinicalDataCount.getValue().equalsIgnoreCase(naValue)) {
                                         return false;
                                       }
                                     }
                                   }
                                   return true;
                                 })
                             .collect(Collectors.toList());
                     clinicalDataCountItem.setCounts(filteredClinicalDataCount);
                     return clinicalDataCountItem;
                   })
               .collect(
                   Collectors.toMap(
                       clinicalDataCountItem -> clinicalDataCountItem.getAttributeId(),
                       clinicalDataCountItem -> clinicalDataCountItem));
            """;

    /*
    public static <T, R> Collector<T, R, R> of(Supplier<R> supplier,
                                               BiConsumer<R, T> accumulator,
                                               BinaryOperator<R> combiner);
    R clearly can become a LinkedHashSet
    evaluation of the lambda must occur with R=LinkedHashSet, as must evaluation of X::combiner
     */
    @Disabled
    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);

    }

}