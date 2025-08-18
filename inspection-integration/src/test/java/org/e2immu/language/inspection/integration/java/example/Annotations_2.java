package org.e2immu.language.inspection.integration.java.example;

import org.e2immu.language.inspection.integration.java.importhelper.a.Resource;
import org.e2immu.language.inspection.integration.java.importhelper.a.Resources;

import static org.e2immu.language.inspection.integration.java.example.Annotations_2.XX;

// this file is here only to show that we can use a static import for XX!
// see TestAnnotations, INPUT3
@Resources({
        @Resource(name = XX, lookup = "yy", type = java.util.TreeMap.class),
        @Resource(name = Annotations_2.ZZ, type = Integer.class)
})
public class Annotations_2 {
    static final String XX = "xx";
    static final String ZZ = "zz";
}
