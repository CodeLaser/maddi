package org.e2immu.language.inspection.openjdk;

import com.sun.tools.javac.code.Flags;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.TypeNature;

public record FlagHelper(Runtime runtime) {

    public void field(long flags, FieldInfo.Builder builder) {
        boolean isPublic = (flags & Flags.PUBLIC) != 0;
        boolean isPrivate = (flags & Flags.PRIVATE) != 0;
        boolean isProtected = (flags & Flags.PROTECTED) != 0;
        boolean isFinal = (flags & Flags.FINAL) != 0;
        boolean isStatic = (flags & Flags.STATIC) != 0;
        boolean isVolatile = (flags & Flags.VOLATILE) != 0;
        boolean isTransient = (flags & Flags.TRANSIENT) != 0;

        if (isPublic) {
            builder.addFieldModifier(runtime.fieldModifierPublic());
        }
        if (isProtected) {
            builder.addFieldModifier(runtime.fieldModifierProtected());
        }
        if (isPrivate) {
            builder.addFieldModifier(runtime.fieldModifierPrivate());
        }
        if (isFinal) {
            builder.addFieldModifier(runtime.fieldModifierFinal());
        }
        if (isStatic) {
            builder.addFieldModifier(runtime.fieldModifierStatic());
        }
        if (isVolatile) {
            builder.addFieldModifier(runtime.fieldModifierVolatile());
        }
        if (isTransient) {
            builder.addFieldModifier(runtime.fieldModifierTransient());
        }
    }

    public void method(long flags, MethodInfo.Builder builder) {
        // The cleaner check: flags directly encode synthetic/bridge/etc.
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

    public MethodInfo.MethodType methodType(long flags) {
        boolean isStatic = (flags & Flags.STATIC) != 0;
        if (isStatic) return runtime().methodTypeStaticMethod();
        boolean isDefault = (flags & Flags.DEFAULT) != 0;
        if (isDefault) return runtime().methodTypeDefaultMethod();
        boolean isAbstract = (flags & Flags.ABSTRACT) != 0;
        if (isAbstract) return runtime().methodTypeAbstractMethod();
        return runtime.methodTypeMethod();
    }

    public void type(long flags, TypeInfo.Builder builder) {
        boolean isPublic = (flags & Flags.PUBLIC) != 0;
        if (isPublic) builder.addTypeModifier(runtime.typeModifierPublic());
        boolean isProtected = (flags & Flags.PROTECTED) != 0;
        if (isProtected) builder.addTypeModifier(runtime.typeModifierProtected());
        boolean isStatic = (flags & Flags.STATIC) != 0;
        if (isStatic) builder.addTypeModifier(runtime.typeModifierStatic());

        boolean isInterface = (flags & Flags.INTERFACE) != 0;
        boolean isRecord = (flags & Flags.RECORD) != 0;
        TypeNature typeNature;
        if (isInterface) {
            typeNature = runtime.typeNatureInterface();
        } else if (isRecord) {
            typeNature = runtime.typeNatureRecord();
        } else {
            typeNature = runtime.typeNatureClass();
        }
        builder.setTypeNature(typeNature);
    }
}
