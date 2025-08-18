package org.e2immu.language.cst.api.info;

import org.e2immu.language.cst.api.output.Qualification;

import java.util.Set;

public interface ImportComputer {
    record Result(Set<String> imports, Qualification qualification) {
    }

    Result go(TypeInfo typeInfo, Qualification qualification);
}
