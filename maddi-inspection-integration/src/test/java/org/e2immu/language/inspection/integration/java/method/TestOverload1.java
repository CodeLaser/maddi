package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestOverload1 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class X {
                public static void add(byte b) {}
                private static void add(byte[] bs) {}
            
                static void test1(byte b) {
                   add(b);
                }
                static void test2(byte[] bs) {
                   add(bs);
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo add1 = typeInfo.methods().getFirst();
        assertTrue(add1.isPubliclyAccessible());
        MethodInfo add2 = typeInfo.methods().get(1);
        assertFalse(add2.isPubliclyAccessible());

        {
            MethodInfo test1 = typeInfo.findUniqueMethod("test1", 1);
            Statement s0 = test1.methodBody().statements().getFirst();
            assertSame(add1, mc(s0).methodInfo());
        }
        {
            MethodInfo test2 = typeInfo.findUniqueMethod("test2", 1);
            Statement s0 = test2.methodBody().statements().getFirst();
            assertSame(add2, mc(s0).methodInfo());
        }
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.security.MessageDigest;
            public class X {
            
                static void test1(MessageDigest md, byte b) {
                   md.update(b);
                }
            
                static void test2(MessageDigest md, byte[] bs) {
                   md.update(bs);
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);

        {
            MethodInfo test1 = typeInfo.findUniqueMethod("test1", 2);
            Statement s0 = test1.methodBody().statements().getFirst();
            assertEquals("java.security.MessageDigest.update(byte)", mc(s0).methodInfo().fullyQualifiedName());
        }
        {
            MethodInfo test2 = typeInfo.findUniqueMethod("test2", 2);
            Statement s0 = test2.methodBody().statements().getFirst();
            assertEquals("java.security.MessageDigest.update(byte[])", mc(s0).methodInfo().fullyQualifiedName());
        }
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            import java.security.MessageDigest;
            public class X {
                static byte[] getBytes() { return new byte[] { 1, 2}; }
                static byte getByte() { return 1; }
            
                static void test1(MessageDigest md) {
                   md.update(getByte());
                }
            
                static void test2(MessageDigest md) {
                   md.update(getBytes());
                }
            
                static void test3(MessageDigest md, String s) {
                   md.update(s.getBytes());
                }
            
                static void test4(MessageDigest md, String s) {
                   md.update(s.getBytes()[0]);
                }
            
                static void test5(MessageDigest md, String s) {
                   byte b = s.getBytes()[0];
                   md.update(b);
                }
            }
            """;

    private static MethodCall mc(Statement s) {
        if (s instanceof ExpressionAsStatement eas) {
            if (eas.expression() instanceof MethodCall mc) {
                return mc;
            }
        }
        throw new UnsupportedOperationException();
    }

    @Test
    public void test3() {
        TypeInfo typeInfo = javaInspector.parse(INPUT3);

        {
            MethodInfo test1 = typeInfo.findUniqueMethod("test1", 1);
            Statement s0 = test1.methodBody().statements().getFirst();
            assertEquals("java.security.MessageDigest.update(byte)", mc(s0).methodInfo().fullyQualifiedName());
        }
        {
            MethodInfo test2 = typeInfo.findUniqueMethod("test2", 1);
            Statement s0 = test2.methodBody().statements().getFirst();
            assertEquals("java.security.MessageDigest.update(byte[])", mc(s0).methodInfo().fullyQualifiedName());
        }
        {
            MethodInfo test3 = typeInfo.findUniqueMethod("test3", 2);
            Statement s0 = test3.methodBody().statements().getFirst();
            assertEquals("java.security.MessageDigest.update(byte[])", mc(s0).methodInfo().fullyQualifiedName());
        }
        {
            MethodInfo test4 = typeInfo.findUniqueMethod("test4", 2);
            Statement s0 = test4.methodBody().statements().get(0);
            assertEquals("java.security.MessageDigest.update(byte)", mc(s0).methodInfo().fullyQualifiedName());
        }
        {
            MethodInfo test5 = typeInfo.findUniqueMethod("test5", 2);
            Statement s1 = test5.methodBody().statements().get(1);
            assertEquals("java.security.MessageDigest.update(byte)", mc(s1).methodInfo().fullyQualifiedName());
        }
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.io.PrintWriter;
            public class X {
            
                interface Expression<Y> {
            
                }
            
                interface CriteriaBuilder {
                     <Y extends Comparable<? super Y>> boolean greaterThan(Expression<? extends Y> expression1,
                         Expression<? extends Y> expression2);
                     <Y extends Comparable<? super Y>> boolean greaterThan(Expression<? extends Y> expression, Y y);
                }
            }
            """;

    @DisplayName("infinite loop type parameter recursion + printing type parameters")
    @Test
    public void test4() {
        TypeInfo typeInfo = javaInspector.parse(INPUT4);


        String s = javaInspector.print2(typeInfo);
        @Language("java")
        String expect = """
                package a.b;
                public class X {
                    interface CriteriaBuilder {
                        <Y extends Comparable<? super Y>> boolean greaterThan(
                            X.Expression<? extends Y> expression1,
                            X.Expression<? extends Y> expression2);
                        <Y extends Comparable<? super Y>> boolean greaterThan(X.Expression<? extends Y> expression, Y y);
                    }
                    interface Expression<Y> { }
                }
                """;
        assertEquals(expect, s);
    }

    @Language("java")
    private static final String INPUT5 = """
            package org.e2immu.analyser.resolver.testexample;
       
            import java.util.Map;
            import java.util.Optional;
            
            public class Overload1_5 {
               interface ITemplateResource { }
               interface IEngineConfiguration { }
               static abstract class StringTemplateResolver {
                   protected ITemplateResource	computeTemplateResource(IEngineConfiguration configuration,
                       String ownerTemplate, String template, Map<String,Object> templateResolutionAttributes);
               }
               static class ContentTemplate {
                   String getContent();
               }
               interface ContentTemplateRepository {
                   Optional<ContentTemplate> findOne(ContentTemplate contentTemplate);
               }

               static abstract class Resolver extends StringTemplateResolver {
                   ContentTemplateRepository contentTemplateRepository;

                   @Override
                   protected ITemplateResource computeTemplateResource(IEngineConfiguration configuration,
                       String ownerTemplate, String template,  Map<String, Object> templateResolutionAttributes) {
                       if (template != null && !template.startsWith("mail/") && !template.startsWith("error")) {
                         ContentTemplate contentTemplate = new ContentTemplate();
                         Optional<ContentTemplate> optionalContentTemplate = contentTemplateRepository.findOne(contentTemplate);
                         return optionalContentTemplate
                           .map(emailTemplate ->
                             // technically the code seems to be correct without the 'Resolver.' prefix
                             super.computeTemplateResource(configuration, ownerTemplate, emailTemplate.getContent(), 
                                 templateResolutionAttributes)
                           )
                           .orElse(null);
                       } else {
                         return null;
                       }
                   }
               }
            }
            """;

    @DisplayName("overload 4 params")
    @Test
    public void test5() {
        javaInspector.parse(INPUT5);
    }
}
