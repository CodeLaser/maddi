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

    /*
    ⚠ STILL OPEN, AND DELIBERATELY NOT ASSERTED HERE — a type-parameterised class nested inside an ANONYMOUS
    class, which this fix does not reach:

        Object m() {
            return new Callable<String>() {
                class Nested<N> { N keep; }
                public String call() { return new Nested<String>().keep; }
            };
        }

    N's owner is Nested, whose owner is the anonymous class: no simple name, so nothing to look up on the
    element stack, and methodLocalType correctly returns null. It failed before this change too (measured:
    'Cannot find element Nested on stack'), and still fails after, only with a more accurate message from the
    next guard along ('Cannot find owner Nested of type variable N'). Not a regression and not fixed; recorded
    so the next reader knows the boundary of what the descent covers rather than discovering it in a corpus.
     */
}
