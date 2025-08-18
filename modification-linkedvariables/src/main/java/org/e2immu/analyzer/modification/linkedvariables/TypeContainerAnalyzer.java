package org.e2immu.analyzer.modification.linkedvariables;

import org.e2immu.annotation.Modified;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.Set;

/*
Level 4.2.

Given the modification state of the parameters of a type, compute the @Container property

 */
public interface TypeContainerAnalyzer extends Analyzer {

    interface Output extends Analyzer.Output {

        Set<MethodInfo> externalWaitForCannotCauseCycles();

    }

    Output go(@Modified TypeInfo typeInfo);

}
