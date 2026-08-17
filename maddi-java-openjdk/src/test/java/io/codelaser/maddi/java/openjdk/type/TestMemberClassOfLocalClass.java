package org.e2immu.language.java.openjdk.type;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>A type declared inside a method body resolves through the element stack — including what is declared INSIDE
 * one.</b>
 * <p>
 * A type variable whose owner has no canonical FQN is resolved by looking the owner up on the element stack,
 * where {@code handleLocalType} registers a method-local class under its simple name. Only the local class
 * ITSELF is registered, so the single-name lookup resolved a local class and nothing nested in one.
 * <p>
 * guava's {@code TypeTokenTest:1413} nests member classes in a local class. {@code X}'s owner is {@code Sub},
 * whose owner is {@code Outer} — a ClassSymbol, not a MethodSymbol — so the lookup asked for "Sub", missed, and
 * threw {@code UnsupportedOperationException: Cannot find element 'Sub' on stack}, which aborts the compilation
 * unit; one aborted unit refuses the whole ParseResult.
 * <p>
 * ⛔ <b>The fallback that was supposed to absorb the miss was DEAD CODE.</b> It read
 * {@code if (elementStack.find(name) instanceof TypeInfo t)}, which looks like it degrades gracefully and does
 * not: {@code find} throws before {@code instanceof} is evaluated. {@code findOrNull} now exists for callers
 * that may legitimately miss.
 * <p>
 * ⚠ Same family as the switch-guard pattern variable pinned by {@code TestSwitchGuardPatternVariable}
 * ({@code Cannot find element 'ai' on stack}): the element stack is missing a declaration form, and it fails by
 * aborting the unit rather than by degrading.
 */
public class TestMemberClassOfLocalClass extends CommonTest {

    /** guava's construct, reduced: BaseWithTypeVar and Outer are BOTH method-local, Sub/Sub2 are members. */
    @Language("java")
    private static final String SRC = """
            package a.b;
            import java.util.List;
            public class Holder {
                void m() {
                    class BaseWithTypeVar<T> {}
                    class Outer<O> {
                        class Sub<X> extends BaseWithTypeVar<List<X>> {}
                        class Sub2<Y extends Sub2<Y>> extends BaseWithTypeVar<List<Y>> {}
                    }
                    Class<?> c = Outer.Sub.class;
                    Class<?> c2 = Outer.Sub2.class;
                    System.out.println(c + "" + c2);
                }
            }
            """;

    /** Two levels of member nesting inside the local class, to check the descent is a walk and not one step. */
    @Language("java")
    private static final String DEEPER = """
            package a.b;
            public class Deeper {
                void m() {
                    class Outer<O> {
                        class Mid<M> {
                            class Inner<I> {
                                I keep;
                            }
                        }
                    }
                    Class<?> c = Outer.Mid.Inner.class;
                    System.out.println(c);
                }
            }
            """;

    /**
     * An ANONYMOUS class in the owner chain: it has no simple name, so the element stack can never hold it.
     * {@code visitNewClass} registers it in {@code typeData} under the compiler's notation instead.
     */
    @Language("java")
    private static final String ANONYMOUS = """
            package a.b;
            import java.util.concurrent.Callable;
            public class Anon {
                Object m() {
                    return new Callable<String>() {
                        class Nested<N> { N keep; }
                        public String call() { return new Nested<String>().keep; }
                    };
                }
            }
            """;

    /** A mixed chain: local class -> anonymous class -> member class, so the walk changes register mid-way. */
    @Language("java")
    private static final String MIXED = """
            package a.b;
            import java.util.concurrent.Callable;
            public class Mixed {
                Object m() {
                    class Local<L> {
                        Object inner() {
                            return new Callable<String>() {
                                class Deep<D> { D keep; }
                                public String call() { return new Deep<String>().keep; }
                            };
                        }
                    }
                    return new Local<String>().inner();
                }
            }
            """;

    @DisplayName("guava's shape: member classes of a method-local class, one with a recursive bound")
    @Test
    public void memberClassesOfALocalClass() {
        TypeInfo holder = scan("a.b.Holder", SRC);
        assertNotNull(holder);
        // the local types live under the method, not under the primary type; reaching the parse at all is the
        // assertion that matters here — pre-fix this throws out of scan()
        assertEquals("Holder", holder.simpleName());
    }

    @DisplayName("the descent walks the whole nesting path, not one level")
    @Test
    public void twoLevelsOfMemberNesting() {
        assertNotNull(scan("a.b.Deeper", DEEPER));
    }

    /**
     * ⛔ The unnamed link. {@code N}'s owner is {@code Nested}, whose owner is the ANONYMOUS class — no simple
     * name, so the element stack could never hold it and a walk that only consulted the stack had to give up.
     * The anonymous type is registered by {@code visitNewClass} in {@code typeData} under the compiler's own
     * notation, so the walk resolves it there and descends as usual.
     */
    @DisplayName("a type-parameterised class nested inside an anonymous class")
    @Test
    public void nestedInsideAnAnonymousClass() {
        assertNotNull(scan("a.b.Anon", ANONYMOUS));
    }

    /** Local class, then anonymous, then member: the walk must change register part-way up one chain. */
    @DisplayName("a mixed chain: local class, anonymous class, member class")
    @Test
    public void mixedOwnerChain() {
        assertNotNull(scan("a.b.Mixed", MIXED));
    }
}
