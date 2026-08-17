package io.codelaser.maddi.cst.impl.runtime;

import io.codelaser.maddi.cst.api.element.SourceSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A source set that claims {@link SourceSet#partOfJdk()} is named after its JPMS module. That is the convention
 * {@code SourceSetImpl.jdkModule(name)} establishes, and a consumer mapping a source set to a module has nothing
 * else to read.
 * <p>
 * {@link PredefinedImpl#PREDEFINED_SOURCESET} was the one counter-example: it claimed {@code partOfJdk()} and
 * answered {@code "<predefined>"}. A downstream module-descriptor generator, asked for the {@code requires} a new
 * module needs, therefore reported {@code <predefined>} among them and then wrote
 * <pre>requires transitive &lt;predefined&gt;;</pre>
 * into a generated {@code module-info.java}, reporting success. The declaration does not lex.
 * <p>
 * ⭐ The assertion below is on the SHAPE of the name, not on the string {@code java.base}: what a consumer needs
 * is that the name is spellable in a {@code requires}, and a test naming the expected literal would keep passing
 * for the next placeholder that happens to differ.
 */
public class TestPredefinedSourceSetName {

    /**
     * A legal module name is a dotted sequence of Java identifiers (JLS 3.9, 7.7). Written out here rather than
     * taken from {@code javax.lang.model.SourceVersion} because this module does not require {@code java.compiler}.
     */
    private static boolean isLegalModuleName(String name) {
        if (name == null || name.isBlank()) return false;
        for (String part : name.split("\\.", -1)) {
            if (part.isEmpty() || !Character.isJavaIdentifierStart(part.charAt(0))) return false;
            for (int i = 1; i < part.length(); i++) {
                if (!Character.isJavaIdentifierPart(part.charAt(i))) return false;
            }
        }
        return true;
    }

    @DisplayName("the predefined source set is named after a module a `requires` can spell")
    @Test
    public void predefinedSourceSetIsNamedAfterItsModule() {
        SourceSet predefined = PredefinedImpl.PREDEFINED_SOURCESET;

        assertTrue(predefined.partOfJdk(), "the convention only applies to a JDK source set");
        assertTrue(isLegalModuleName(predefined.name()),
                () -> "'" + predefined.name() + "' cannot be written in a `requires`");
    }

    /**
     * ⚠ The control on the check itself: a predicate that accepted everything would make the test above pass
     * whatever the name were. This is the exact name that shipped, and it must be rejected.
     */
    @DisplayName("CONTROL: the predicate rejects the placeholder that used to be returned")
    @Test
    public void thePredicateRejectsThePlaceholder() {
        assertTrue(!isLegalModuleName("<predefined>"));
        assertTrue(!isLegalModuleName(""));
        assertTrue(!isLegalModuleName("   "));
        assertTrue(!isLegalModuleName(":libs:core"));
        assertTrue(!isLegalModuleName("a..b"));
        assertTrue(isLegalModuleName("java.base"));
        assertTrue(isLegalModuleName("org.apache.lucene.core"));
    }

    /**
     * Every type the predefined source set holds is a primitive, which needs no {@code requires} at all, or a
     * {@code java.lang} type, which is in {@code java.base} — so the name is accurate and not merely legal. The
     * census is on the compilation units, because that is what would change if a type from another module were
     * ever added here.
     */
    @DisplayName("and the name is accurate: nothing here comes from outside java.base")
    @Test
    public void everyPredefinedTypeBelongsToThatModule() {
        PredefinedImpl predefined = new PredefinedImpl();

        predefined.predefinedObjects().forEach(typeInfo -> {
            String packageName = typeInfo.compilationUnit().packageName();
            assertTrue(packageName == null || packageName.isEmpty() || "java.lang".equals(packageName),
                    () -> typeInfo.fullyQualifiedName() + " is in '" + packageName
                          + "', which is not covered by " + PredefinedImpl.PREDEFINED_SOURCESET.name());
        });
        assertEquals("java.base", PredefinedImpl.PREDEFINED_SOURCESET.name());
    }
}
