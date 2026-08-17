package io.codelaser.maddi.java.openjdk.print;

import io.codelaser.maddi.cst.api.info.ImportComputer;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code ImportComputer.addStaticImport}: a caller that pastes verbatim text can ask for a static import.
 *
 * <p>The computer derives imports from the TYPES a unit references, which is the whole answer for a printer —
 * it renders {@code String.format(...)} qualified, so a static import of {@code format} is dead and is dropped,
 * which {@code TestVariousPrint2Issues} 'static import issue' pins deliberately. A caller pasting verbatim text
 * has the opposite need: the text says {@code asList(...)} and nothing in the CST names anything that would
 * bring the import back. Only that caller knows, so it says so, exactly as it already does for a type with
 * {@link ImportComputerImpl#add}.
 *
 * <p>Found from splitclass, where the split copies a member's text into a new type and computes that type's
 * imports: three of the plain (non-EJB) class-isolate corpus trees are test-shaped POJOs calling
 * {@code assertThat(...)}, and the emitted parts carried none of the original's static imports — 209 of the
 * 400 errors in emitted files, from this one cause.
 */
public class TestStaticImports extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import java.util.List;
            import static java.util.Arrays.asList;
            import static java.util.Collections.*;
            class X {
                List<String> method() {
                    return unmodifiableList(asList("a", "b"));
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = scan("a.b.X", INPUT1);
        Qualification qualification = runtime.qualificationExistingSources();

        // not asked for: the static imports are dropped, because a printer qualifies every member reference
        ImportComputer plain = new ImportComputerImpl();
        assertEquals("java.util.Arrays, java.util.Collections, java.util.List",
                imports(plain.go(X.compilationUnit(), qualification)));

        // asked for: carried, and printed as 'import static ...;'
        ImportComputer withStatics = new ImportComputerImpl();
        withStatics.addStaticImport("java.util.Arrays.asList");
        withStatics.addStaticImport("java.util.Collections.*");
        assertEquals("java.util.Arrays, java.util.Collections, java.util.List,"
                     + " static java.util.Arrays.asList, static java.util.Collections.*",
                imports(withStatics.go(X.compilationUnit(), qualification)));
    }

    private static String imports(ImportComputer.Result r) {
        return r.imports().stream().map(ImportComputer.ImportDetails::importString)
                .collect(Collectors.joining(", "));
    }
}
