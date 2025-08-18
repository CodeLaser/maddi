package org.e2immu.language.cst.impl.output;

import org.e2immu.language.cst.api.output.FormattingOptions;
import org.e2immu.language.cst.api.output.element.Keyword;

public record KeywordImpl(String keyword) implements Keyword {

    public static final Keyword ABSTRACT = new KeywordImpl("abstract");
    public static final Keyword ASSERT = new KeywordImpl("assert");
    public static final Keyword AT_INTERFACE = new KeywordImpl("@interface");
    public static final Keyword BREAK = new KeywordImpl("break");
    public static final Keyword CASE = new KeywordImpl("case");
    public static final Keyword CATCH = new KeywordImpl("catch");
    public static final Keyword CLASS = new KeywordImpl("class");
    public static final Keyword CONTINUE = new KeywordImpl("continue");
    public static final Keyword DEFAULT = new KeywordImpl("default");
    public static final Keyword DO = new KeywordImpl("do");
    public static final Keyword ELSE = new KeywordImpl("else");
    public static final Keyword ENUM = new KeywordImpl("enum");
    public static final Keyword EXTENDS = new KeywordImpl("extends");
    public static final Keyword FINAL = new KeywordImpl("final");
    public static final Keyword FINALLY = new KeywordImpl("finally");
    public static final Keyword FOR = new KeywordImpl("for");
    public static final Keyword GOTO = new KeywordImpl("goto");
    public static final Keyword IF = new KeywordImpl("if");
    public static final Keyword IMPLEMENTS = new KeywordImpl("implements");
    public static final Keyword IMPORT = new KeywordImpl("import");
    public static final Keyword INTERFACE = new KeywordImpl("interface");
    public static final Keyword LENGTH = new KeywordImpl("length");
    public static final Keyword NATIVE = new KeywordImpl("native");
    public static final Keyword NEW = new KeywordImpl("new");
    public static final Keyword NON_SEALED = new KeywordImpl("non-sealed");
    public static final Keyword NULL = new KeywordImpl("null");
    public static final Keyword PACKAGE = new KeywordImpl("package");
    public static final Keyword PRIVATE = new KeywordImpl("private");
    public static final Keyword PROTECTED = new KeywordImpl("protected");
    public static final Keyword PUBLIC = new KeywordImpl("public");
    public static final Keyword RECORD = new KeywordImpl("record");
    public static final Keyword RETURN = new KeywordImpl("return");
    public static final Keyword SEALED = new KeywordImpl("sealed");
    public static final Keyword STATIC = new KeywordImpl("static");
    public static final Keyword STRICTFP = new KeywordImpl("strictfp");
    public static final Keyword SUPER = new KeywordImpl("super");
    public static final Keyword SWITCH = new KeywordImpl("switch");
    public static final Keyword SYNCHRONIZED = new KeywordImpl("synchronized");
    public static final Keyword THIS = new KeywordImpl("this");
    public static final Keyword THROW = new KeywordImpl("throw");
    public static final Keyword THROWS = new KeywordImpl("throws");
    public static final Keyword TRANSIENT = new KeywordImpl("transient");
    public static final Keyword TRY = new KeywordImpl("try");
    public static final Keyword VAR = new KeywordImpl("var");
    public static final Keyword VOLATILE = new KeywordImpl("volatile");
    public static final Keyword WHILE = new KeywordImpl("while");
    public static final Keyword YIELD = new KeywordImpl("yield");

    @Override
    public String minimal() {
        return keyword;
    }

    @Override
    public String write(FormattingOptions options) {
        return keyword;
    }

    @Override
    public String generateJavaForDebugging() {
        return ".add(KeywordImpl." + keyword.toUpperCase() + ")";
    }

    @Override
    public String toString() {
        return keyword;
    }
}
