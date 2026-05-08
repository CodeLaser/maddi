package org.e2immu.language.inspection.openjdk;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.TypeNature;

public record FlagHelper(Runtime runtime) {

    public void method(JCTree.JCMethodDecl jcMethod, MethodInfo.Builder builder) {
        // The cleaner check: flags directly encode synthetic/bridge/etc.
        long flags = jcMethod.getModifiers().flags;
        boolean isSynthetic = (flags & Flags.SYNTHETIC) != 0;
        boolean isBridge = (flags & Flags.BRIDGE) != 0;
        boolean isGeneratedConstructor = (flags & Flags.GENERATEDCONSTR) != 0;
        boolean isPublic = (flags & Flags.PUBLIC) != 0;
        boolean isPrivate = (flags & Flags.PRIVATE) != 0;
        boolean isProtected = (flags & Flags.PROTECTED) != 0;
        if (isPublic) {
            builder.addMethodModifier(runtime.methodModifierPublic());
        }
        if (isPrivate) {
            builder.addMethodModifier(runtime.methodModifierPrivate());
        }
        if (isProtected) {
            builder.addMethodModifier(runtime.methodModifierProtected());
        }
        if (isSynthetic || isGeneratedConstructor) {
            builder.setSynthetic(true);
        }
    }

    public void type(long flags, TypeInfo.Builder builder) {
        boolean isPublic = (flags & Flags.PUBLIC) != 0;
        if (isPublic) builder.addTypeModifier(runtime.typeModifierPublic());
        boolean isProtected = (flags & Flags.PROTECTED) != 0;
        if (isProtected) builder.addTypeModifier(runtime.typeModifierProtected());
        boolean isStatic = (flags & Flags.STATIC) != 0;
        if (isStatic) builder.addTypeModifier(runtime.typeModifierStatic());

        boolean isInterface = (flags & Flags.INTERFACE) != 0;
        TypeNature typeNature;
        if (isInterface) {
            typeNature = runtime.typeNatureInterface();
        } else {
            typeNature = runtime.typeNatureClass();
        }
        builder.setTypeNature(typeNature);
    }
}
