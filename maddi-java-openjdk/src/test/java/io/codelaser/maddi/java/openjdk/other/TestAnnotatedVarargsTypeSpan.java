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

package io.codelaser.maddi.java.openjdk.other;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;

/**
 * The detailed source of the type named in a varargs parameter whose ellipsis carries an annotation,
 * {@code Alpha @NonNull ... alphas}: javac ends {@code Alpha} after {@code @NonNull}, and refactor-base's MoveType,
 * replacing that span by {@code a.Alpha}, wrote {@code @NonNull a.Alpha ... alphas}, which does not compile. The
 * plain varargs and the annotated array forms were right and must stay so.
 */
public class TestAnnotatedVarargsTypeSpan extends CommonTest {

    private static final String NON_NULL = """
            package ann;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target(ElementType.TYPE_USE)
            public @interface NonNull {}
            """;

    private static final String ALPHA = """
            package a;
            public class Alpha {}
            """;

    private static final String VARARGS = """
            package stay;
            import a.Alpha;
            import ann.NonNull;
            import java.util.List;
            public class Varargs {
                public void withAlphas(@NonNull Alpha @NonNull ... alphas) { }
                public void plain(Alpha... alphas) { }
                public void array(@NonNull Alpha @NonNull [] alphas) { }
                public void qualified(a.@NonNull Alpha @NonNull ... alphas) { }
                public void generic(List<Alpha> @NonNull ... alphas) { }
                public void twoDims(Alpha @NonNull [] @NonNull ... alphas) { }
                public void noLeading(Alpha /* c */ @NonNull ... alphas) { }
            }
            """;

    private TypeInfo varargs;

    @BeforeEach
    public void beforeEach() {
        varargs = scan(false, Map.of("ann.NonNull", NON_NULL, "a.Alpha", ALPHA, "stay.Varargs", VARARGS))
                .primaryTypes().stream().filter(t -> "stay.Varargs".equals(t.fullyQualifiedName())).findFirst()
                .orElseThrow();
    }

    private ParameterInfo parameter(String method) {
        return varargs.methods().stream().filter(m -> method.equals(m.name())).findFirst().orElseThrow()
                .parameters().getFirst();
    }

    private static String span(ParameterInfo p, TypeInfo typeInfo) {
        return p.source().detailedSources().details(typeInfo).toString();
    }

    private String alphaSpan(String method) {
        ParameterInfo p = parameter(method);
        return span(p, p.parameterizedType().typeInfo());
    }

    @Test
    public void theTypeEndsBeforeTheVarargsAnnotation() {
        assertEquals("[-@6:37-6:41]", alphaSpan("withAlphas"), "`Alpha`, not `Alpha @NonNull`");
        assertEquals("[-@9:27-9:42]", alphaSpan("qualified"), "`a.@NonNull Alpha`");
        assertEquals("[-@12:27-12:31]", alphaSpan("noLeading"), "`Alpha`, not over the comment either");
    }

    @Test
    public void theParameterKeepsItsWholeSpan() {
        assertEquals("-@6:28-6:61", parameter("withAlphas").source().toString());
        assertEquals("-@12:27-12:59", parameter("noLeading").source().toString());
    }

    @Test
    public void theFormsThatWereRightStaySo() {
        assertEquals("[-@7:23-7:27]", alphaSpan("plain"));
        assertEquals("[-@8:32-8:36]", alphaSpan("array"));
        assertEquals("[-@11:25-11:29]", alphaSpan("twoDims"));
        ParameterInfo generic = parameter("generic");
        assertEquals("[-@10:25-10:28]", span(generic, generic.parameterizedType().typeInfo()), "`List`");
        assertEquals("[-@10:30-10:34]", span(generic,
                generic.parameterizedType().parameters().getFirst().typeInfo()), "`Alpha` in `List<Alpha>`");
    }
}
