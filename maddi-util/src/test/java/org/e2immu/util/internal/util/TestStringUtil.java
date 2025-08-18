package org.e2immu.util.internal.util;

import org.junit.jupiter.api.Test;

import static org.e2immu.util.internal.util.StringUtil.replaceSlashDollar;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestStringUtil {
    @Test
    public void testReplaceSlashDollar() {
        assertEquals("org.e2immu.sequence", replaceSlashDollar("org/e2immu/sequence"));
        assertEquals("org.e2immu.sequence", replaceSlashDollar("org/e2immu$sequence"));
        assertEquals("org.e2immu.$sequence", replaceSlashDollar("org/e2immu$$sequence"));
    }
}
