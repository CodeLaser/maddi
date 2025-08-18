package org.e2immu.language.inspection.integration.java.method;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
