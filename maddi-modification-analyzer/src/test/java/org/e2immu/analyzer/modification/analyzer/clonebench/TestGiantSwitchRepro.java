package org.e2immu.analyzer.modification.analyzer.clonebench;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/*
Performance regression guard for the Function18752956_file2311713 grind-to-halt (a ~100-arm switch in a loop:
equal-quality witness ties are the COMMON case, and the witness-index tie-break/support machinery dominated).
Baseline history: 583s (support-set printing in the tie-break) -> 100s (two-fact string keys) -> 48s (cheap
vertex printer + witness key cache) -> 25s (lazy witness support, cached Fact hash) -> ~13s (structural
tie-break via vertexComparator, no strings). Skipped when the testarchive checkout is absent.
 */
public class TestGiantSwitchRepro extends CommonTest {

    @Test
    public void test() throws Exception {
        Path file = Path.of(
                "../../testarchive/switch_fors_compiles/src/main/java/Function18752956_file2311713.java");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(file),
                "requires the testarchive checkout ('analyzed' branch)");
        String source = Files.readString(file);
        long start = System.currentTimeMillis();
        TypeInfo t = javaInspector.parse("Function18752956_file2311713", source);
        System.out.println("REPRO parse " + (System.currentTimeMillis() - start) + "ms");
        long prepStart = System.currentTimeMillis();
        List<Info> ao = prepWork(t);
        System.out.println("REPRO prep " + (System.currentTimeMillis() - prepStart) + "ms");
        long anStart = System.currentTimeMillis();
        analyzer.go(ao);
        System.out.println("REPRO analyze " + (System.currentTimeMillis() - anStart) + "ms");
    }
}
