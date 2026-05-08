package org.e2immu.language.inspection.openjdk;

import com.sun.source.tree.*;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import java.io.PrintStream;

class AnalysisScanner extends TreePathScanner<Void, Void> {

    private final Trees trees;
    private final PrintStream out;
    private int depth = 0;

    AnalysisScanner(Trees trees, PrintStream out) {
        this.trees = trees;
        this.out = out;
    }

    // -- Indentation helper ----------------------------------------------

    private String indent() {
        return "  ".repeat(depth);
    }

    private void print(String label, String value) {
        out.printf("%s%-22s %s%n", indent(), label, value);
    }

    // -- Depth tracking --------------------------------------------------
    //
    // Override scan() to bracket every node visit with depth tracking.
    // You can skip this and just use depth++ / depth-- in each visitXxx
    // pair if you only care about specific node types.

    @Override
    public Void scan(Tree tree, Void p) {
        depth++;
        Void result = super.scan(tree, p);
        depth--;
        return result;
    }

    // -- Class declarations ----------------------------------------------

    @Override
    public Void visitClass(ClassTree node, Void p) {
        out.println();
        print("CLASS:", node.getSimpleName().toString());
        return super.visitClass(node, p);   // visit children
    }

    // -- Method declarations ---------------------------------------------

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        out.println();
        print("METHOD:", node.getName().toString());

        // getElement() gives you the javax.lang.model.element.Element for
        // this declaration — here an ExecutableElement for the method.
        // You can cast it to query parameter types, return type, throws, etc.
        var element = trees.getElement(getCurrentPath());
        if (element != null) {
            print("  element kind:", element.getKind().toString());
            print("  return type:", element.asType().toString());
        }
        return super.visitMethod(node, p);
    }

    // -- Variable declarations (fields and locals) -----------------------

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        print("VARIABLE:", node.getName().toString());

        var typeMirror = trees.getTypeMirror(getCurrentPath());
        if (typeMirror != null) {
            print("  type:", typeMirror.toString());
        }
        return super.visitVariable(node, p);
    }

    // -- Method calls ----------------------------------------------------
    //
    // This is probably the most important node for code-quality analysis.
    // After attribution, getElement() on a method invocation path gives
    // you the ExecutableElement of the *resolved* method — including the
    // declaring class, parameter types, and return type — even through
    // overloading and generics.

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        // The method select is usually a MemberSelectTree ("obj.method")
        // or an IdentifierTree ("method"), both giving the method name.
        String callSite = node.getMethodSelect().toString();
        print("CALL:", callSite);

        var element = trees.getElement(getCurrentPath());
        if (element != null) {
            print("  resolves to:", element.toString());
            print("  declared in:", element.getEnclosingElement().toString());
        }

        // getTypeMirror on a method invocation gives you the *return type*
        // of the call expression as seen by the type checker.
        var returnType = trees.getTypeMirror(getCurrentPath());
        if (returnType != null) {
            print("  return type:", returnType.toString());
        }
        return super.visitMethodInvocation(node, p);
    }

    // -- Identifier references -------------------------------------------
    //
    // An identifier is any bare name: a local variable, a field, a class
    // name, a type parameter, etc.  getElement() tells you which one.

    @Override
    public Void visitIdentifier(IdentifierTree node, Void p) {
        var element = trees.getElement(getCurrentPath());
        if (element != null) {
            // Filter out TYPE and PACKAGE identifiers for brevity;
            // you probably want all of these in a real analyzer.
            switch (element.getKind()) {
                case LOCAL_VARIABLE, PARAMETER, FIELD, ENUM_CONSTANT -> {
                    print("IDENT:", node.getName().toString());
                    print("  kind:", element.getKind().toString());
                    var type = trees.getTypeMirror(getCurrentPath());
                    if (type != null) print("  type:", type.toString());
                }
                default -> { /* skip type/package refs for brevity */ }
            }
        }
        return super.visitIdentifier(node, p);
    }

    // -- If you want to see EVERY node, uncomment this: ------------------
    //
    // @Override
    // public Void visitOther(Tree node, Void p) {
    //     print("NODE:", node.getKind().toString());
    //     return super.visitOther(node, p);
    // }
    //
    // Or override scan() to print every node kind before super.scan():
    //
    // @Override
    // public Void scan(Tree tree, Void p) {
    //     if (tree != null)
    //         out.printf("%s[%s]%n", "  ".repeat(depth), tree.getKind());
    //     depth++;
    //     Void result = super.scan(tree, p);
    //     depth--;
    //     return result;
    // }
}
