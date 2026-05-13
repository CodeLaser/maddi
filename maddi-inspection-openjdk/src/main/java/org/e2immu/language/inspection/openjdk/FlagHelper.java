package org.e2immu.language.inspection.openjdk;

import com.sun.tools.javac.code.Flags;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.TypeNature;

public record FlagHelper(Runtime runtime) {

    public void field(long flags, FieldInfo.Builder builder) {
        if ((flags & Flags.PUBLIC) != 0) builder.addFieldModifier(runtime.fieldModifierPublic());
        if ((flags & Flags.PROTECTED) != 0) builder.addFieldModifier(runtime.fieldModifierProtected());
        if ((flags & Flags.PRIVATE) != 0) builder.addFieldModifier(runtime.fieldModifierPrivate());
        if ((flags & Flags.FINAL) != 0) builder.addFieldModifier(runtime.fieldModifierFinal());
        if ((flags & Flags.STATIC) != 0) builder.addFieldModifier(runtime.fieldModifierStatic());
        if ((flags & Flags.VOLATILE) != 0) builder.addFieldModifier(runtime.fieldModifierVolatile());
        if ((flags & Flags.TRANSIENT) != 0) builder.addFieldModifier(runtime.fieldModifierTransient());
    }

    public void method(long flags, MethodInfo.Builder builder) {
        if ((flags & Flags.PUBLIC) != 0) builder.addMethodModifier(runtime.methodModifierPublic());
        if ((flags & Flags.PRIVATE) != 0) builder.addMethodModifier(runtime.methodModifierPrivate());
        if ((flags & Flags.PROTECTED) != 0) builder.addMethodModifier(runtime.methodModifierProtected());
        if ((flags & Flags.FINAL) != 0) builder.addMethodModifier(runtime.methodModifierFinal());
        if ((flags & Flags.SYNCHRONIZED) != 0) builder.addMethodModifier(runtime.methodModifierSynchronized());
        if ((flags & Flags.STATIC) != 0) builder.addMethodModifier(runtime.methodModifierStatic());
        if ((flags & Flags.ABSTRACT) != 0) builder.addMethodModifier(runtime.methodModifierAbstract());
        if ((flags & Flags.DEFAULT) != 0) builder.addMethodModifier(runtime.methodModifierDefault());

        boolean isSynthetic = (flags & Flags.SYNTHETIC) != 0;
        boolean isBridge = (flags & Flags.BRIDGE) != 0;
        boolean isGeneratedConstructor = (flags & Flags.GENERATEDCONSTR) != 0;
        if (isSynthetic || isGeneratedConstructor || isBridge) {
            builder.setSynthetic(true);
        }
    }

    public MethodInfo.MethodType methodType(long flags, boolean inInterface) {
        if ((flags & Flags.DEFAULT) != 0) return runtime().methodTypeDefaultMethod();
        if ((flags & Flags.STATIC) != 0) return runtime().methodTypeStaticMethod();
        if ((flags & Flags.ABSTRACT) != 0 || inInterface) return runtime().methodTypeAbstractMethod();
        return runtime.methodTypeMethod();
    }

    public void type(long flags, TypeInfo.Builder builder, String simpleName) {
        if ((flags & Flags.PUBLIC) != 0) builder.addTypeModifier(runtime.typeModifierPublic());
        if ((flags & Flags.PROTECTED) != 0) builder.addTypeModifier(runtime.typeModifierProtected());
        if ((flags & Flags.PROTECTED) != 0) builder.addTypeModifier(runtime.typeModifierPrivate());
        if ((flags & Flags.STATIC) != 0) builder.addTypeModifier(runtime.typeModifierStatic());
        if ((flags & Flags.ABSTRACT) != 0) builder.addTypeModifier(runtime.typeModifierAbstract());
        if ((flags & Flags.SEALED) != 0) builder.addTypeModifier(runtime.typeModifierSealed());
        if ((flags & Flags.NON_SEALED) != 0) builder.addTypeModifier(runtime.typeModifierNonSealed());
        if ((flags & Flags.FINAL) != 0) builder.addTypeModifier(runtime.typeModifierFinal());

        TypeNature typeNature;
        if ((flags & Flags.INTERFACE) != 0) {
            if (simpleName.startsWith("@")) {
                typeNature = runtime.typeNatureAnnotation();
            } else {
                typeNature = runtime.typeNatureInterface();
            }
        } else if ((flags & Flags.RECORD) != 0) {
            typeNature = runtime.typeNatureRecord();
        } else if ((flags & Flags.ENUM) != 0) {
            typeNature = runtime.typeNatureEnum();
        } else {
            typeNature = runtime.typeNatureClass();
        }
        builder.setTypeNature(typeNature);
    }
}
