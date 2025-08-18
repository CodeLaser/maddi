package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.LocalVariable;

import java.util.List;

public interface TryStatement extends Statement {

    interface CatchClause extends Element {
        List<ParameterizedType> exceptionTypes();

        boolean isFinal();

        CatchClause rewire(InfoMap infoMap);

        CatchClause translate(TranslationMap translationMap);

        LocalVariable catchVariable();

        Block block();

        CatchClause withBlock(Block newBlock);

        interface Builder extends Element.Builder<Builder> {

            @Fluent
            Builder setBlock(Block block);

            @Fluent
            Builder addType(ParameterizedType type);

            @Fluent
            Builder setFinal(boolean isFinal);

            @Fluent
            Builder setCatchVariable(LocalVariable catchVariable);

            CatchClause build();
        }
    }

    Block finallyBlock();

    List<CatchClause> catchClauses();

    /* either LVC or EAS with VE */
    List<Statement> resources();

    interface Builder extends Statement.Builder<Builder> {

        @Fluent
        Builder setBlock(Block block);

        @Fluent
        Builder setFinallyBlock(Block block);

        @Fluent
        Builder addCatchClause(CatchClause catchClause);

        @Fluent
        Builder addResource(Statement resource);

        TryStatement build();
    }

    String NAME = "try";

    @Override
    default String name() {
        return NAME;
    }
}
