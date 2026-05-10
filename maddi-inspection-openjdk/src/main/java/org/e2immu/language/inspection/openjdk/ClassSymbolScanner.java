package org.e2immu.language.inspection.openjdk;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassSymbolScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassSymbolScanner.class);
    private final Runtime runtime;
    private final FlagHelper flagHelper;
    private final Elements elements;
    private final TypeData typeData;
    private ConvertType convertType;
    private final Set<TypeInfo> recursionPrevention = new HashSet<>();

    public ClassSymbolScanner(Runtime runtime,
                              FlagHelper flagHelper,
                              Elements elements,
                              TypeData typeData) {
        this.runtime = runtime;
        this.flagHelper = flagHelper;
        this.elements = elements;
        this.typeData = typeData;
    }

    public void setConvertType(ConvertType convertType) {
        assert this.convertType == null : "SetOnce!";
        this.convertType = convertType;
    }

    TypeInfo typeInfo(Symbol.ClassSymbol cs) {
        String packageName = cs.owner.toString();
        boolean internal = cs.classfile == null;
        URI uri;
        if (internal) {
            uri = URI.create("jrt:/internal/");
        } else {
            uri = cs.classfile.toUri();
        }
        SourceSet sourceSet = ensureSourceSet(uri);
        CompilationUnit cu = runtime.newCompilationUnitBuilder()
                .setPackageName(packageName)
                .setSourceSet(sourceSet)
                .setURI(uri)
                .build();
        String simpleName = cs.name.toString();
        TypeInfo newTypeInfo = runtime.newTypeInfo(cu, simpleName);
        typeData.put(newTypeInfo);
        if (!internal) {
            loadType(cs, newTypeInfo);
        }
        return newTypeInfo;
    }

    private void loadType(Symbol.ClassSymbol cs, TypeInfo newTypeInfo) {
        flagHelper.type(cs.flags(), newTypeInfo.builder());
        if (recursionPrevention.add(newTypeInfo)) {
            //The following completely loads 'cs'
            List<? extends Element> members = elements.getAllMembers(cs);

            int index = 0;
            for (Symbol.TypeVariableSymbol typeParameter : cs.getTypeParameters()) {
                TypeParameter newTp = runtime.newTypeParameter(index++, typeParameter.getSimpleName().toString(), newTypeInfo);
                newTypeInfo.builder().addOrSetTypeParameter(newTp);
            }

            ParameterizedType superType = convertType.convert(cs.getSuperclass());
            newTypeInfo.builder().setParentClass(superType);

            for (Type type : cs.getInterfaces()) {
                ParameterizedType pt = convertType.convert(type);
                newTypeInfo.builder().addInterfaceImplemented(pt);
            }

            for (var member : members) {
                addMemberToType(newTypeInfo, cs, member);
            }
            recursionPrevention.remove(newTypeInfo);
        }
    }

    private static final Pattern JAR_FILE = Pattern.compile("(jar:file:.+)/([^/!]+)!/.*");
    private static final Pattern JAVA_RUNTIME = Pattern.compile("jrt:/([^/]+)/.*");

    private SourceSet ensureSourceSet(URI uri) {
        Matcher m = JAR_FILE.matcher(uri.toString());
        if (m.matches()) {
            String jarName = m.group(2);
            URI jarUri = URI.create(m.group(1) + "/" + m.group(2));
            SourceSet known = typeData.getSourceSet(jarName);
            if (known == null) {
                SourceSet sourceSet = new SourceSetImpl(jarName, jarUri, false, false, true,
                        true, false);
                typeData.put(sourceSet);
                return sourceSet;
            }
            return known;
        }
        Matcher rt = JAVA_RUNTIME.matcher(uri.toString());
        if (rt.matches()) {
            String module = rt.group(1);
            URI jarUri = URI.create("jmod:" + module);
            SourceSet known = typeData.getSourceSet(module);
            if (known == null) {
                SourceSet sourceSet = new SourceSetImpl(module, jarUri, false, false, true,
                        true, true);
                typeData.put(sourceSet);
                return sourceSet;
            }
            return known;
        }
        throw new UnsupportedOperationException("NYI");
    }

    private void addMemberToType(TypeInfo typeInfo, Symbol.ClassSymbol owner, Element member) {
        if (member instanceof Symbol.MethodSymbol ms && ms.owner == owner) {
            addMethodToType(typeInfo, ms);
        } else if (member instanceof Symbol.VarSymbol vs && vs.owner == owner) {
            addFieldToType(typeInfo, vs);
        } else if (member instanceof Symbol.ClassSymbol cs && cs.owner == owner) {
            addEnclosedTypeToType(typeInfo, cs);
        }
    }

    private void addEnclosedTypeToType(TypeInfo typeInfo, Symbol.ClassSymbol cs) {
        String name = cs.getSimpleName().toString();
        LOGGER.info("Adding enclosed type {} to {}", name, typeInfo);
        TypeInfo enclosed = runtime.newTypeInfo(typeInfo, name);
        typeData.put(enclosed);
        typeInfo.builder().addSubType(enclosed);
        loadType(cs, enclosed);
    }

    private void addFieldToType(TypeInfo typeInfo, Symbol.VarSymbol vs) {
        String name = vs.getSimpleName().toString();
        LOGGER.info("Adding field {} to {}", name, typeInfo);
        ParameterizedType type = convertType.convert(vs.type);
        boolean isStatic = (vs.flags() & Flags.STATIC) != 0;
        FieldInfo fieldInfo = runtime.newFieldInfo(name, isStatic, type, typeInfo);
        typeInfo.builder().addField(fieldInfo);
        flagHelper.field(vs.flags(), fieldInfo.builder());

        typeData.put(vs, fieldInfo);
    }

    private void addMethodToType(TypeInfo typeInfo, Symbol.MethodSymbol ms) {
        String name = ms.getSimpleName().toString();
        MethodInfo method;
        if ("<init>".equals(name)) {
            LOGGER.info("Adding constructor {} to {}", name, typeInfo);
            method = runtime.newConstructor(typeInfo);
        } else {
            LOGGER.info("Adding method {} to {}", name, typeInfo);
            MethodInfo.MethodType methodType = flagHelper.methodType(ms.flags());
            method = runtime.newMethod(typeInfo, name, methodType);
            typeInfo.builder().addMethod(method);
        }
        int index = 0;
        for (Symbol.TypeVariableSymbol typeParameter : ms.getTypeParameters()) {
            TypeParameter newTp = runtime.newTypeParameter(index++, typeParameter.getSimpleName().toString(), method);
            method.builder().addTypeParameter(newTp);
            typeData.putTmpMethodTypeParameter(typeInfo.fullyQualifiedName(), newTp.simpleName(), newTp);
        }

        flagHelper.method(ms.flags(), method.builder());
        for (Symbol.VarSymbol parameter : ms.params) {
            ParameterizedType pt = convertType.convert(parameter.type);
            ParameterInfo parameterInfo = method.builder().addParameter(parameter.getSimpleName().toString(), pt);
            long flags = parameter.flags();
            if ((flags & Flags.VARARGS) != 0) parameterInfo.builder().setVarArgs(true);
            if ((flags & Flags.FINAL) != 0) parameterInfo.builder().setIsFinal(true);
            parameterInfo.builder().commit();
        }
        method.builder().commitParameters();
        // now the fully qualified name has been computed...

        typeData.clearTmpMethodTypeParameterMap(typeInfo.fullyQualifiedName());
        typeData.put(ms, method);
    }

}
