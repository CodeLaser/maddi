package org.e2immu.analyzer.modification.linkedvariables;

import org.e2immu.annotation.Modified;
import org.e2immu.language.cst.api.info.TypeInfo;

/*
Level 5.
Given @Container, @Immutable, do @UtilityClass, @Singleton
 */
public interface TypeMiscAnalyzer extends Analyzer {

    Output go(@Modified TypeInfo typeInfo);
}
