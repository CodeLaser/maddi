package org.e2immu.language.cst.api.output.element;

public interface TypeName extends Qualifier {
    /*
     If true, prefix the type name with @.
     We do this to ensure that the still dodgy output system never splits a line between @ and the name.
     This method can be removed later, in favour of a Symbol.AT to be printed in AnnotationExpressionImpl.print(...)
     */
    boolean annotation();

}
