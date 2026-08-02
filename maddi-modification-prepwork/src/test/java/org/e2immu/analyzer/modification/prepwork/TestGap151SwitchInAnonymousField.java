package org.e2immu.analyzer.modification.prepwork;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;

import static org.junit.jupiter.api.Assertions.*;

/**
 * #151 PROBE — reducing the Elasticsearch shape until the trigger is isolated.
 * <p>
 * {@code RepositoryAnalyzeAction.AsyncAction.finalRegisterValueVerifier(String,int,Random,Releasable)}
 * crashes the prep analyzer AFTER a clean parse with
 * {@code NullPointerException: Cannot invoke "Variable.fullyQualifiedName()" because "variable" is null}.
 * <p>
 * ⚠ The shape is NOT guessed from the gap text — it was read out of the corpus. The method returns an
 * anonymous {@code Runnable} whose FIELD INITIALIZER is a SWITCH EXPRESSION whose arms are themselves
 * anonymous classes capturing the enclosing method's parameters, with a {@code default ->} block arm that
 * completes abruptly (assert + throw) instead of yielding a value.
 * <p>
 * Each candidate feature gets its own test so the crash is attributed to a feature and not to the pile.
 */
public class TestGap151SwitchInAnonymousField extends CommonTest {

    /** The full shape, reduced to the four features listed above and nothing else. */
    @Language("java")
    private static final String FULL = """
            package a.b;
            import java.util.Random;
            class X {
                interface Consumer { void accept(String s); }
                void use(String s) { }
                Runnable make(String name, int expected, Random random) {
                    return new Runnable() {
                        final Consumer reader = switch (random.nextInt(3)) {
                            case 0 -> new Consumer() {
                                @Override public void accept(String s) { use(name); }
                            };
                            case 1 -> new Consumer() {
                                @Override public void accept(String s) { use(name + expected); }
                            };
                            default -> {
                                assert false;
                                throw new IllegalStateException();
                            }
                        };
                        @Override public void run() { reader.accept(name); }
                    };
                }
            }
            """;

    /** Feature dropped: the arms are no longer anonymous classes. */
    @Language("java")
    private static final String NO_ANON_ARMS = """
            package a.b;
            import java.util.Random;
            class X {
                void use(String s) { }
                Runnable make(String name, int expected, Random random) {
                    return new Runnable() {
                        final String reader = switch (random.nextInt(3)) {
                            case 0 -> name;
                            case 1 -> name + expected;
                            default -> {
                                assert false;
                                throw new IllegalStateException();
                            }
                        };
                        @Override public void run() { use(reader); }
                    };
                }
            }
            """;

    /** Feature dropped: the abruptly-completing default arm yields a value instead. */
    @Language("java")
    private static final String NO_ABRUPT_DEFAULT = """
            package a.b;
            import java.util.Random;
            class X {
                interface Consumer { void accept(String s); }
                void use(String s) { }
                Runnable make(String name, int expected, Random random) {
                    return new Runnable() {
                        final Consumer reader = switch (random.nextInt(3)) {
                            case 0 -> new Consumer() {
                                @Override public void accept(String s) { use(name); }
                            };
                            default -> new Consumer() {
                                @Override public void accept(String s) { use(name + expected); }
                            };
                        };
                        @Override public void run() { reader.accept(name); }
                    };
                }
            }
            """;

    /** Feature dropped: the switch is in a METHOD body, not a field initializer of the anonymous class. */
    @Language("java")
    private static final String SWITCH_IN_METHOD = """
            package a.b;
            import java.util.Random;
            class X {
                interface Consumer { void accept(String s); }
                void use(String s) { }
                Runnable make(String name, int expected, Random random) {
                    return new Runnable() {
                        @Override public void run() {
                            Consumer reader = switch (random.nextInt(3)) {
                                case 0 -> new Consumer() {
                                    @Override public void accept(String s) { use(name); }
                                };
                                default -> {
                                    assert false;
                                    throw new IllegalStateException();
                                }
                            };
                            reader.accept(name);
                        }
                    };
                }
            }
            """;

    /**
     * ⛔ NOT MERELY "DOES NOT THROW". A guard that skipped the initializer entirely would also stop the crash,
     * and would be a silence in place of a defect. So the analysis must be shown to have RUN: the anonymous
     * type's field carries a committed {@code analysisOfInitializer}.
     */
    private void prep(String input) {
        TypeInfo X = javaInspector.parse("a.b.X", input);
        new PrepAnalyzer(runtime).doPrimaryType(X);

        var make = X.findUniqueMethod("make", 3);
        var rs = (org.e2immu.language.cst.api.statement.ReturnStatement) make.methodBody().statements().getFirst();
        var cc = (org.e2immu.language.cst.api.expression.ConstructorCall) rs.expression();
        TypeInfo anon = cc.anonymousClass();
        assertNotNull(anon, "the returned Runnable is an anonymous class");
        assertFalse(anon.fields().isEmpty(), "the anonymous class declares the initialized field");
        anon.fields().forEach(f -> assertTrue(
                f.analysisOfInitializer().haveAnalyzedValueFor(VariableDataImpl.VARIABLE_DATA),
                "the initializer of " + f + " must be ANALYZED, not skipped: a guard that merely"
                + " avoided the crash by not walking the field would pass a does-not-throw test"));
    }

    @DisplayName("#151: the full shape — anonymous field initializer, switch expression, abrupt arm")
    @Test
    public void full() {
        prep(FULL);
    }

    @DisplayName("#151: the switch ARMS being anonymous is NOT the trigger — this still crashed before the fix")
    @Test
    public void noAnonArms() {
        prep(NO_ANON_ARMS);
    }

    @DisplayName("#151: an arm that YIELDS instead of throwing never reached the null return variable")
    @Test
    public void noAbruptDefault() {
        prep(NO_ABRUPT_DEFAULT);
    }

    @DisplayName("#151: in a METHOD body there is a return variable, so the same switch is fine")
    @Test
    public void switchInMethod() {
        TypeInfo X = javaInspector.parse("a.b.X", SWITCH_IN_METHOD);
        new PrepAnalyzer(runtime).doPrimaryType(X);   // no field initializer here: the point is the contrast
    }
}
