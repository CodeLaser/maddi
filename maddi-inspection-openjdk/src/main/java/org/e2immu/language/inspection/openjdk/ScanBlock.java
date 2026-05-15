package org.e2immu.language.inspection.openjdk;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.LocalVariable;

import java.util.List;
import java.util.Map;

public record ScanBlock(ScanData sd) {

    Block parseBlock(String blockIndex, Tree node, LocalVariable... variablesToAdd) {
        List<JCTree.JCStatement> statements;
        Source source = sd.statementSourceForNode(node);

        switch (node) {
            case JCTree.JCBlock block -> statements = block.stats;
            case JCTree.JCStatement statement -> statements = List.of(statement);
            case null -> {
                return sd.runtime().newBlockBuilder()
                        .setSource(source)
                        .addComments(sd.commentsForNode(source))
                        .build();
            }
            default -> throw new UnsupportedOperationException("NYI");
        }
        return parseBlock(blockIndex, statements, sd.statementLabels().get(node), source, variablesToAdd);
    }

    private Block parseBlock(String blockIndex,
                             List<JCTree.JCStatement> statements,
                             String label,
                             Source source,
                             LocalVariable... variablesToAdd) {
        Map<String, Element> localVariableMap = sd.elementStack().push();
        for (LocalVariable lv : variablesToAdd) {
            localVariableMap.put(lv.simpleName(), lv);
        }
        int n = statements.size();
        BlockBuilders bb = sd.blockBuilders();
        String i = "-".equals(blockIndex) ? "-" : bb.statementIndex() + "." + blockIndex;
        bb.addLast(new BlockData(runtime.newBlockBuilder(), i, n));

        for (JCTree.JCStatement statement : statements) {
            if (statement instanceof JCTree.JCBlock subBlock) {
                Block parsedSub = parseBlock("0", subBlock);
                bb.addStatement(parsedSub);
            } else if (statement instanceof JCTree.JCClassDecl localType) {
                Statement localTypeCreation = handleLocalType(localType);
                bb.addStatement(localTypeCreation);
            } else {
                scan(statement, null);
            }
        }

        sd.elementStack().pop();

        return bb.removeLast()
                .setLabel(label)
                .setSource(source)
                .addTrailingComments(sd.trailingCommentsForNode(source))
                .addComments(sd.commentsForNode(source))
                .build();
    }


    private Statement handleLocalType(JCTree.JCClassDecl localType) {
        String simpleName = localType.getSimpleName().toString();
        int index = currentMethod.typeInfo().builder().getAndIncrementAnonymousTypes();
        TypeInfo typeInfo = sd.runtime().newTypeInfo(currentMethod, simpleName, index);
        continueType(typeInfo, localType);
        typeInfo.builder()
                .setAccess(sd.runtime().accessPrivate())
                .commit();
        sd.elementStack().put(simpleName, typeInfo);
        return sd.runtime().newLocalTypeDeclarationBuilder()
                .setLabel(sd.statementLabels().get(localType))
                .setTypeInfo(typeInfo)
                .setSource(sd.statementSourceForNode(localType))
                .build();
    }

}
