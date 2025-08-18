package org.e2immu.language.cst.impl.output;

import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.element.QualifiedName;
import org.e2immu.language.cst.api.output.element.Qualifier;
import org.e2immu.language.cst.api.output.element.ThisName;
import org.e2immu.language.cst.api.output.element.TypeName;
import org.e2immu.util.internal.util.StringUtil;

public record QualifiedNameImpl(String name, Qualifier qualifier, Required qualifierRequired) implements QualifiedName {

    // for tests
    public QualifiedNameImpl(String name) {
        this(name, null, Required.NEVER);
    }

    public enum Required {
        YES, // always write
        NO_FIELD, // don't write unless a field-related option says so
        NO_METHOD, // don't write unless a method-related option says so
        NEVER // never write
    }

    @Override
    public String minimal() {
        return qualifierRequired == Required.YES ? qualifier.minimal() + "." + name : name;
    }

    @Override
    public String fullyQualifiedName() {
        return qualifier.fullyQualifiedName() + "." + name;
    }

    @Override
    public int length(FormattingOptions options) {
        return minimal().length();
    }

    @Override
    public String write(FormattingOptions options) {
        if (options.allFieldsRequireThis() && qualifierRequired == Required.NO_FIELD
            && qualifier instanceof ThisName) {
            return qualifier().write(options) + "." + name;
        }
        if (options.allStaticFieldsRequireType() && qualifierRequired != Required.NO_FIELD
            && qualifier instanceof TypeName) {
            return qualifier().write(options) + "." + name;
        }
        return minimal();
    }

    @Override
    public String generateJavaForDebugging() {
        String q = qualifier == null ? "null" : StringUtil.quote(qualifier.minimal());
        return ".add(new QualifiedNameImpl(" + StringUtil.quote(name) + ", " + q + ", " + qualifierRequired + "))";
    }
}
