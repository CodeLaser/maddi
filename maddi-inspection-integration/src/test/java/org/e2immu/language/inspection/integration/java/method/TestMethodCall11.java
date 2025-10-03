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

public class TestMethodCall11 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            
            import java.lang.annotation.Annotation;
            import java.util.Collection;
            import java.util.LinkedHashSet;
            import java.util.NoSuchElementException;
            import java.util.Set;
            import java.util.stream.Collector;
            
            class X {
                public interface MergedAnnotation<A extends Annotation> {
                    A synthesize() throws NoSuchElementException;
                }
                public static <A extends Annotation> Collector<MergedAnnotation<A>, ?, Set<A>> toAnnotationSet() {
                    return Collector.of(LinkedHashSet::new, (set, annotation) -> set.add(annotation.synthesize()),
                            X::combiner);
                }
            
                private static <E, C extends Collection<E>> C combiner(C collection, C additions) {
                	collection.addAll(additions);
                	return collection;
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

    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import org.springframework.core.io.buffer.DataBuffer;
            import org.springframework.core.io.support.ResourceRegion;
            import java.util.function.Function;
            
            abstract class X {
                interface Subscriber<T> {
                    void onComplete();
                    void onError(Throwable t);
                    void onNext(T t);
                }
                interface Publisher<T> {
                     void subscribe(Subscriber<? super T> subscriber);
                }
                interface Flux<R> { }
                static class Mono<T> {
                    static <T> Mono<T> from(Publisher<? extends T> source);
                    <R> Flux<R> flatMapMany(Function<? super T, ? extends Publisher<? extends R>> function);
                }
                abstract Flux<DataBuffer> dataBuffer();
                Flux<DataBuffer> method(Publisher<? extends ResourceRegion> input) {
                	if (input instanceof Mono) {
                			return Mono.from(input)
                					.flatMapMany(region -> {
                						if(region.getResource().isReadable()) {
                                            return dataBuffer();
                                        }
                                        return null;
                					});
                	}
                    return null;
                }
            }
            """;

    // problem is genericsHelper.translateMap(...), special branch for functional interfaces on Mono.from(input).
    // works fine if Publisher is not void subscribe(...) method, but e.g. T get();
    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);
        TypeInfo publisher = typeInfo.findSubType("Publisher");
        assertTrue(publisher.isFunctionalInterface());
    }


    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            
            class Container {
                interface TypeX {}
                interface XX {}
                interface TypeY {}
                interface YY {}
            
                static class P {
                    YY toProto(TypeY y) {
                        return null;
                    }
                    XX toProto(TypeX x) {
                        return null;
                    }
                }
                static class C extends P {
                    String toProto(Object o) {
                        return "s";
                    }
                }
                static class I {
                    void method(C c, TypeY y) {
                       acceptYY(c.toProto(y));
                    }
                    YY acceptYY(YY yy) {
                      return yy;
                    }
            
                }
            }
            """;

    @DisplayName("Balance hierarchy vs argument fit")
    @Test
    public void test3() {
        javaInspector.parse(INPUT3);
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.util.concurrent.Future;
            import org.junit.jupiter.api.Assertions;
            class Container {
                void method(Future<Boolean> f1, Future<Boolean> f2) {
                    Assertions.assertTrue(f1.get() ^ f2.get());
                }
            }
            """;

    @DisplayName("boolean operator and futures")
    @Test
    public void test4() {
        javaInspector.parse(INPUT4);
    }


    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            
            class C {
                interface JBuilder {
                    JBuilder setSize(int i);
                    JBuilder setLabel(String label);
                }
                interface Customizer {
                    void customize(JBuilder jBuilder);
                }
                static class Configuration {
                    public Customizer customizer() {
                        return jBuilder -> jBuilder.setSize(30);
                    }
                }
            }
            """;

    @DisplayName("type forwarding")
    @Test
    public void test5() {
        TypeInfo C = javaInspector.parse(INPUT5);
        TypeInfo JBuilder = C.findSubType("JBuilder");
        assertFalse(JBuilder.isFunctionalInterface());
        TypeInfo Customizer = C.findSubType("Customizer");
        assertTrue(Customizer.isFunctionalInterface());
        TypeInfo Configuration = C.findSubType("Configuration");
        assertFalse(Configuration.isFunctionalInterface());
        MethodInfo customizer = Configuration.findUniqueMethod("customizer", 0);
        Lambda lambda = (Lambda) customizer.methodBody().statements().getFirst().expression();
        assertEquals("a.b.C.Customizer", lambda.abstractFunctionalTypeInfo().toString());
    }


    @Language("java")
    private static final String INPUT6 = """
            package a.b;
            
            import java.util.Arrays;
            
            class C {
                interface CommandArguments {
                    default CommandArguments add(byte[] b) { return this; }
                    default CommandArguments add(int i) { return this; }
                    default CommandArguments addObjects(Object... objects) { return this; }
                    default CommandArguments processKeys(byte[]... keys) { return this; }
                    default CommandArguments processKeys(String... keys) { return this; }
                 }
                static class CommandObject<T> {
                    public CommandObject(CommandArguments args) { }
                }
                int EVAL = 1;
                CommandArguments commandArguments(int i) { return new CommandArguments() {}; }

                public final CommandObject<Object> eval(byte[] script, int keyCount, byte[]... params) {
                    return new CommandObject<>(commandArguments(EVAL).add(script).add(keyCount)
                        .addObjects((Object[]) params).processKeys(Arrays.copyOf(params, keyCount)));
                }
            }
            """;

    @DisplayName("vararg arrays")
    @Test
    public void test6() {
        TypeInfo C = javaInspector.parse(INPUT6);
    }
    
    @Language("java")
    private static final String INPUT7 = """
            package a.b;
            
            import java.util.ArrayList;
            import java.util.List;
            import java.util.Map;
            import java.util.concurrent.atomic.AtomicLong;
            import java.util.function.ToDoubleFunction;
            class C {
                interface ApplicationMetric { }
                Map<ApplicationMetric, AtomicLong> stringAtomicLongMap;
                interface MetricSample { }
                static class GaugeMetricSample<T> implements MetricSample {
                    public GaugeMetricSample(T value, ToDoubleFunction<T> apply) { }
                }
                public List<MetricSample> export(ApplicationMetric registerKeyMetric) {
                    List<MetricSample> list = new ArrayList<>();
                    list.add(new GaugeMetricSample<>(stringAtomicLongMap, value -> value.get(registerKeyMetric).get()));
                    return list;
                }
            }
            """;

    @DisplayName("forwards again")
    @Test
    public void test7() {
        javaInspector.parse(INPUT7);
    }


    @Language("java")
    private static final String INPUT8 = """
            package a.b;
            class X {
                interface SetKv {
                    void set(String k, String v);
                }
                interface Setter<C> { void set(C c, String s1, String s2); }
                static class SetterClass<C> {
                    SetterClass(Setter<C> setter, int k) {
                        // nothing here
                    }
                }
                static class Use extends SetterClass<SetKv> {
                    Use(SetKv d) {
                        super((s, k, v)-> s.set(k, v), 1);
                    }
                }
            }
            """;

    @DisplayName("lambda in super call")
    @Test
    public void test8() {
        javaInspector.parse(INPUT8);
    }

    @Language("java")
    private static final String INPUT8b = """
            package a.b;
            class X {
                interface SetKv {
                    void set(String k, String v);
                }
                interface Setter<C> { void set(C c, String s1, String s2); }
                static class SetterClass<C> {
                    void callSetter(Setter<C> setter, int k) {
                        // nothing here
                    }
                }
                static class Use extends SetterClass<SetKv> {
                    Use(SetKv d) {
                        callSetter((s, k, v)-> s.set(k, v), 1);
                    }
                }
            }
            """;

    @DisplayName("lambda in normal method call")
    @Test
    public void test8b() {
        javaInspector.parse(INPUT8b);
    }
}
