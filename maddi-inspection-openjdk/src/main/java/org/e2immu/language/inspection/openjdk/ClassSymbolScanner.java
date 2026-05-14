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
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassSymbolScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassSymbolScanner.class);
    private final Runtime runtime;
    private final FlagHelper flagHelper;
    private final Elements elements;
    private final TypeData typeData;
    private final SourceSet sourceSetOfCurrentTask;
    private ConvertType convertType;
    private final Set<TypeInfo> recursionPrevention = new HashSet<>();
    private final Map<String, TypeInfo> predefinedTypes = new HashMap<>();
    private final ElementStack elementStack;

    public ClassSymbolScanner(Runtime runtime,
                              SourceSet sourceSetOfCurrentTask,
                              FlagHelper flagHelper,
                              Elements elements,
                              TypeData typeData,
                              ElementStack elementStack) {
        this.runtime = runtime;
        this.flagHelper = flagHelper;
        this.elements = elements;
        this.typeData = typeData;
        this.sourceSetOfCurrentTask = sourceSetOfCurrentTask;
        this.elementStack = elementStack; // for local types owned by a method

        predefinedTypes.put("String", runtime.stringTypeInfo());
        predefinedTypes.put("Object", runtime.objectTypeInfo());
        predefinedTypes.put("Integer", runtime.integerTypeInfo());
        predefinedTypes.put("Boolean", runtime.boxedBooleanTypeInfo());
        predefinedTypes.put("Long", runtime.boxedLongTypeInfo());
        predefinedTypes.put("Character", runtime.characterTypeInfo());
        predefinedTypes.put("Class", runtime.classTypeInfo());
    }

    public void setConvertType(ConvertType convertType) {
        assert this.convertType == null : "SetOnce!";
        this.convertType = convertType;
    }

    TypeInfo type(Symbol.ClassSymbol cs) {
        switch (cs.owner) {
            case Symbol.PackageSymbol _ -> {
                return primaryType(cs, false);
            }
            case Symbol.ClassSymbol enclosedSymbol -> {
                TypeInfo owner = convertType.convert(enclosedSymbol.type).typeInfo();
                String simpleName = cs.getSimpleName().toString();
                TypeInfo inMap = owner.findSubType(simpleName, false);
                if (inMap == null) {
                    TypeInfo enclosed = runtime.newTypeInfo(owner, simpleName);
                    // note: first put the type in typeData, only then load it... self-references are common!
                    typeData.put(enclosed);
                    loadType(cs, enclosed, false);
                    owner.builder().addSubType(enclosed);
                    return enclosed;
                }
                return inMap;
            }
            case Symbol.MethodSymbol _ -> {
                return (TypeInfo) elementStack.find(cs.getSimpleName().toString());
            }
            case null, default -> throw new UnsupportedOperationException();
        }
    }

    TypeInfo primaryType(Symbol.ClassSymbol cs, boolean loadMembers) {
        String simpleName = cs.name.toString();
        assert cs.owner instanceof Symbol.PackageSymbol;
        String packageName = cs.owner.toString();
        TypeInfo newTypeInfo;
        boolean internal;
        TypeInfo predefinedType;
        if ("java.lang".equals(packageName) && (predefinedType = predefinedTypes.get(simpleName)) != null) {
            newTypeInfo = predefinedType;
            internal = false;
        } else {
            internal = cs.classfile == null;
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
            newTypeInfo = runtime.newTypeInfo(cu, simpleName);
        }
        typeData.put(newTypeInfo);
        if (!internal) {
            loadType(cs, newTypeInfo, loadMembers);
        }
        return newTypeInfo;
    }

    private void loadType(Symbol.ClassSymbol cs, TypeInfo newTypeInfo, boolean loadMembers) {
        flagHelper.type(cs.flags(), newTypeInfo.builder(), newTypeInfo.simpleName());
        if (recursionPrevention.add(newTypeInfo)) {
            //The following completely loads 'cs'
            List<? extends Element> members = elements.getAllMembers(cs);

            int index = 0;
            convertType.newTypeParameterMap();
            for (Symbol.TypeVariableSymbol typeParameter : cs.getTypeParameters()) {
                TypeParameter newTp = runtime.newTypeParameter(index++, typeParameter.getSimpleName().toString(), newTypeInfo);
                convertType.putInLastTypeParameterMap(newTp);

                List<ParameterizedType> bounds = new ArrayList<>();
                if (typeParameter.type instanceof Type.TypeVar tv) {
                    Type lowerBound = tv.getLowerBound();
                    if (lowerBound.getKind() != TypeKind.NULL) {
                        throw new UnsupportedOperationException();
                    } else {
                        Type upperBound = tv.getUpperBound();
                        if (upperBound.getKind() != TypeKind.NULL) {
                            if (upperBound.tsym == cs) {
                                // self reference, as in java.lang.Enum<E extends Enum<E>>
                                bounds.add(runtime.newParameterizedType(newTypeInfo,
                                        List.of(runtime.newParameterizedType(newTp, 0, null))));
                            } else if (upperBound instanceof Type.ClassType ct) {
                                ParameterizedType upper = convertType.convert(ct);
                                if (!upper.isJavaLangObject()) {
                                    bounds.add(upper.withWildcard(runtime.wildcardExtends()));
                                }
                            } else {
                                throw new UnsupportedOperationException();
                            }
                        }
                    }
                }
                newTp.builder().setTypeBounds(List.copyOf(bounds)).commit();
                newTypeInfo.builder().addOrSetTypeParameter(newTp);
            }
            convertType.popTypeParameterMap();

            ParameterizedType superType = convertType.convert(cs.getSuperclass());
            newTypeInfo.builder().setParentClass(superType);

            for (Type type : cs.getInterfaces()) {
                ParameterizedType pt = convertType.convert(type);
                newTypeInfo.builder().addInterfaceImplemented(pt);
            }

            if (loadMembers) {
                for (var member : members) {
                    addMemberToType(newTypeInfo, cs, member);
                }
                MethodInfo singleAbstractMethod = convertType.computeSAM(cs.type);
                newTypeInfo.builder().setSingleAbstractMethod(singleAbstractMethod);
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
        return sourceSetOfCurrentTask;
    }

    private void addMemberToType(TypeInfo typeInfo, Symbol.ClassSymbol owner, Element member) {
        if (member instanceof Symbol.MethodSymbol ms && ms.owner == owner) {
            boolean isPublic = (ms.flags() & Flags.PUBLIC) != 0;
            if (isPublic) {
                addMethodToType(typeInfo, ms);
            }
        } else if (member instanceof Symbol.VarSymbol vs && vs.owner == owner) {
            boolean isPublic = (vs.flags() & Flags.PUBLIC) != 0;
            if (isPublic) {
                addFieldToType(typeInfo, vs);
            }
        } else if (member instanceof Symbol.ClassSymbol cs && cs.owner == owner) {
            boolean isPublic = (cs.flags() & Flags.PUBLIC) != 0;
            if (isPublic) {
                addEnclosedTypeToType(typeInfo, cs);
            }
        }
    }

    private void addEnclosedTypeToType(TypeInfo typeInfo, Symbol.ClassSymbol cs) {
        if (typeData.getType(cs.fullname.toString()) != null) return;
        String name = cs.getSimpleName().toString();
        LOGGER.debug("Adding enclosed type {} to {}", name, typeInfo);
        TypeInfo enclosed = runtime.newTypeInfo(typeInfo, name);
        typeData.put(enclosed);
        typeInfo.builder().addSubType(enclosed);
        loadType(cs, enclosed, true);
    }

    private void addFieldToType(TypeInfo typeInfo, Symbol.VarSymbol vs) {
        String name = vs.getSimpleName().toString();
        LOGGER.debug("Adding field {} to {}", name, typeInfo);
        ParameterizedType type = convertType.convert(vs.type);
        boolean isStatic = (vs.flags() & Flags.STATIC) != 0;
        FieldInfo fieldInfo = runtime.newFieldInfo(name, isStatic, type, typeInfo);
        typeInfo.builder().addField(fieldInfo);
        flagHelper.field(vs.flags(), fieldInfo.builder());

        typeData.put(vs, fieldInfo);
    }

    MethodInfo addMethodToType(TypeInfo typeInfo, Symbol.MethodSymbol ms) {
        String name = ms.getSimpleName().toString();
        MethodInfo method;
        if ("<init>".equals(name)) {
            LOGGER.debug("Adding constructor {} to {}", name, typeInfo);
            method = runtime.newConstructor(typeInfo);
        } else {
            LOGGER.debug("Adding method {} to {}", name, typeInfo);
            MethodInfo.MethodType methodType = flagHelper.methodType(ms.flags(), typeInfo.isInterface());
            method = runtime.newMethod(typeInfo, name, methodType);
            typeInfo.builder().addMethod(method);
        }
        int index = 0;
        MethodInfo.Builder builder = method.builder();

        for (Symbol.TypeVariableSymbol typeParameter : ms.getTypeParameters()) {
            TypeParameter newTp = runtime.newTypeParameter(index++, typeParameter.getSimpleName().toString(), method);
            builder.addTypeParameter(newTp);
            typeData.putTmpMethodTypeParameter(typeInfo.fullyQualifiedName(), newTp.simpleName(), newTp);
        }

        flagHelper.method(ms.flags(), builder);
        if (ms.params != null) {
            for (Symbol.VarSymbol parameter : ms.params) {
                ParameterizedType pt = convertType.convert(parameter.type);
                ParameterInfo parameterInfo = builder.addParameter(parameter.getSimpleName().toString(), pt);
                long flags = parameter.flags();
                if ((flags & Flags.VARARGS) != 0) parameterInfo.builder().setVarArgs(true);
                if ((flags & Flags.FINAL) != 0) parameterInfo.builder().setIsFinal(true);
                parameterInfo.builder().commit();
            }
        }
        ParameterizedType returnType = convertType.convert(ms.getReturnType());
        builder.setReturnType(returnType);

        builder.commitParameters();
        // now the fully qualified name has been computed...

        typeData.clearTmpMethodTypeParameterMap(typeInfo.fullyQualifiedName());
        typeData.put(ms, method);

        return method;
    }

}
