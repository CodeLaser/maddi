package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.type.TypeNature;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.Resolver;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.language.inspection.impl.parser.ContextImpl;
import org.e2immu.language.inspection.impl.parser.ResolverImpl;
import org.e2immu.language.inspection.impl.parser.SummaryImpl;
import org.e2immu.language.inspection.impl.parser.TypeContextImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.e2immu.support.Either;
import org.parsers.java.JavaParser;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;


public class CommonTestParse {

    private TypeInfo predefined(String fullyQualifiedName, boolean complain) {
        return switch (fullyQualifiedName) {
            case "java.lang.Class" -> runtime.classTypeInfo();
            case "java.lang.Object" -> runtime.objectTypeInfo();
            case "java.lang.Long" -> runtime.boxedLongTypeInfo();
            case "java.lang.String" -> runtime.stringTypeInfo();
            case "java.lang.Override" -> override;
            case "java.lang.Integer" -> runtime.integerTypeInfo();
            case "java.lang.SuppressWarnings" -> suppressWarnings;
            case "java.lang.System" -> system;
            case "java.lang.Math" -> math;
            case "java.lang.Exception" -> exception;
            case "java.lang.RuntimeException" -> runtimeException;
            case "java.lang.Enum" -> enumTypeInfo;
            case "java.lang.AutoCloseable" -> autoCloseable;
            case "java.io.PrintStream" -> printStream;
            case "java.util.function.Function" -> function;
            case "java.util.function.Consumer" -> consumer;
            case "java.util.function.BiConsumer" -> biConsumer;
            case "java.util.Hashtable" -> hashtable;
            case "java.lang.annotation.Annotation" -> annotationClass;
            default -> {
                if (complain) throw new UnsupportedOperationException("Type " + fullyQualifiedName);
                yield null;
            }
        };
    }

    protected final Runtime runtime = new RuntimeImpl() {
        @Override
        public TypeInfo getFullyQualified(String name, boolean complain) {
            return predefined(name, complain);
        }

        @Override
        public TypeInfo syntheticFunctionalType(int inputParameters, boolean hasReturnValue) {
            if (inputParameters == 1 && hasReturnValue) return function;
            if (inputParameters == 1) return consumer;
            if (inputParameters == 2 && !hasReturnValue) return biConsumer;
            throw new UnsupportedOperationException();
        }
    };

    protected final TypeInfo math;
    protected final TypeInfo system;
    protected final TypeInfo exception;
    protected final TypeInfo runtimeException;
    protected final TypeInfo printStream;
    protected final TypeInfo function;
    protected final TypeInfo consumer;
    protected final TypeInfo biConsumer;
    protected final TypeInfo suppressWarnings;
    protected final TypeInfo override;
    protected final TypeInfo enumTypeInfo;
    protected final TypeInfo autoCloseable;
    protected final TypeInfo hashtable;
    protected final TypeInfo annotationClass;

    class CompiledTypesManagerImpl implements CompiledTypesManager {
        private final Map<String, TypeInfo> typeMap;

        CompiledTypesManagerImpl(Map<String, TypeInfo> typeMap) {
            this.typeMap = typeMap;
        }

        @Override
        public void addTypeInfo(SourceFile sf, TypeInfo typeInfo) {
            typeMap.put(typeInfo.fullyQualifiedName(), typeInfo);
        }

        @Override
        public TypeData typeDataOrNull(String fqn, SourceSet sourceSet) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TypeInfo get(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
            TypeInfo inMap = typeMap.get(fullyQualifiedName);
            if (inMap != null) return inMap;
            return predefined(fullyQualifiedName, false);
        }

        @Override
        public List<TypeInfo> primaryTypesInPackageEnsureLoaded(String packageName, SourceSet sourceSetOfRequest) {
            return typeMap.values().stream().filter(ti -> packageName.equals(ti.packageName())).toList();
        }
    }

    protected CommonTestParse() {
        SourceSet sourceSet = new SourceSetImpl("hardcoded", List.of(), URI.create("file:/"),
                StandardCharsets.UTF_8, false, true, true, true, false,
                Set.of(), Set.of());
        CompilationUnit javaLang = runtime.newCompilationUnitBuilder().setPackageName("java.lang")
                .setSourceSet(sourceSet).build();
        CompilationUnit javaIo = runtime.newCompilationUnitBuilder().setPackageName("java.io")
                .setSourceSet(sourceSet).build();
        CompilationUnit javaUtil = runtime.newCompilationUnitBuilder().setPackageName("java.util")
                .setSourceSet(sourceSet).build();
        CompilationUnit javaUtilFunction = runtime.newCompilationUnitBuilder().setPackageName("java.util.function")
                .setSourceSet(sourceSet).build();
        CompilationUnit javaLangAnnotation = runtime.newCompilationUnitBuilder().setPackageName("java.lang.annotation")
                .setSourceSet(sourceSet).build();

        suppressWarnings = runtime.newTypeInfo(javaLang, "SuppressWarnings");
        enumTypeInfo = runtime.newTypeInfo(javaLang, "Enum");
        math = runtime.newTypeInfo(javaLang, "Math");
        override = runtime.newTypeInfo(javaLang, "Override");
        printStream = runtime.newTypeInfo(javaIo, "PrintStream");
        system = runtime.newTypeInfo(javaLang, "System");
        exception = runtime.newTypeInfo(javaLang, "Exception");
        runtimeException = runtime.newTypeInfo(javaLang, "RuntimeException");
        function = runtime.newTypeInfo(javaUtilFunction, "Function");
        consumer = runtime.newTypeInfo(javaUtilFunction, "Consumer");
        biConsumer = runtime.newTypeInfo(javaUtilFunction, "BiConsumer");
        autoCloseable = runtime.newTypeInfo(javaLang, "AutoCloseable");
        hashtable = runtime.newTypeInfo(javaUtil, "Hashtable");
        annotationClass = runtime.newTypeInfo(javaLangAnnotation, "Annotation");

        TypeParameter C = runtime.newTypeParameter(0, "C", runtime.classTypeInfo());
        C.builder().commit();
        runtime.classTypeInfo().builder().addOrSetTypeParameter(C);
        MethodInfo getClass = runtime.newMethod(runtime.objectTypeInfo(), "getClass", runtime.methodTypeMethod());
        getClass.builder()
                .setAccess(runtime.accessPublic())
                .setReturnType(runtime.newParameterizedType(runtime.classTypeInfo(), List.of(runtime.parameterizedTypeWildcard())))
                .commitParameters();
        MethodInfo clone = runtime.newMethod(runtime.objectTypeInfo(), "clone", runtime.methodTypeAbstractMethod());
        clone.builder()
                .setAccess(runtime.accessProtected())
                .setReturnType(runtime.newParameterizedType(runtime.objectTypeInfo(), 0))
                .commitParameters();
        MethodInfo toString = runtime.newMethod(runtime.objectTypeInfo(), "toString", runtime.methodTypeMethod());
        toString.builder().setAccess(runtime.accessPublic()).setReturnType(runtime.stringParameterizedType()).commitParameters();
        runtime.objectTypeInfo().builder().addMethod(getClass).addMethod(clone).addMethod(toString).hierarchyIsDone();

        defineFunction();
        defineConsumer();
        defineBiConsumer();

        MethodInfo equals = runtime.newMethod(runtime.objectTypeInfo(), "equals", runtime.methodTypeMethod());
        equals.builder().addParameter("other", runtime.objectParameterizedType());
        equals.builder()
                .setAccess(runtime.accessPublic())
                .addMethodModifier(runtime.methodModifierPublic())
                .setReturnType(runtime.booleanParameterizedType())
                .commitParameters().commit();
        runtime.objectTypeInfo().builder().addMethod(equals);

        MethodInfo pow = runtime.newMethod(math, "pow", runtime.methodTypeStaticMethod());
        pow.builder().addParameter("base", runtime.doubleParameterizedType());
        pow.builder().addParameter("exponent", runtime.doubleParameterizedType());
        pow.builder().setReturnType(runtime.doubleParameterizedType());
        pow.builder().commit();
        TypeNature typeNatureClass = runtime.typeNatureClass();

        math.builder().setTypeNature(typeNatureClass).addMethod(pow).setParentClass(runtime.objectParameterizedType());
        math.builder().commit();

        MethodInfo printlnString = runtime.newMethod(printStream, "println", runtime.methodTypeMethod());
        printlnString.builder().addParameter("string", runtime.stringParameterizedType());
        printlnString.builder().setReturnType(runtime.voidParameterizedType());
        printlnString.builder().commit();
        printStream.builder().addMethod(printlnString);

        MethodInfo printlnInt = runtime.newMethod(printStream, "println", runtime.methodTypeMethod());
        printlnInt.builder().addParameter("i", runtime.intParameterizedType());
        printlnInt.builder().setReturnType(runtime.voidParameterizedType());
        printlnInt.builder().commit();
        printStream.builder().addMethod(printlnInt);

        MethodInfo printlnObject = runtime.newMethod(printStream, "println", runtime.methodTypeMethod());
        printlnObject.builder().addParameter("object", runtime.objectParameterizedType());
        printlnObject.builder().setReturnType(runtime.voidParameterizedType());
        printlnObject.builder().commit();
        printStream.builder().addMethod(printlnObject);

        printStream.builder().setTypeNature(typeNatureClass).setParentClass(runtime.objectParameterizedType());
        printStream.builder().commit();

        system.builder().setTypeNature(typeNatureClass).setParentClass(runtime.objectParameterizedType());

        FieldInfo out = runtime.newFieldInfo("out", true, printStream.asSimpleParameterizedType(), system);
        system.builder().addField(out);
        system.builder().commit();

        override.builder().setTypeNature(runtime.typeNatureAnnotation());

        TypeParameter E = runtime.newTypeParameter(0, "E", enumTypeInfo);
        E.builder().commit();
        enumTypeInfo.builder()
                .setTypeNature(runtime.typeNatureClass())
                .setParentClass(runtime.objectParameterizedType())
                .addOrSetTypeParameter(E);
        enumTypeInfo.builder().commit();

        exception.builder().setTypeNature(typeNatureClass);

        MethodInfo rteConstructor = runtime.newConstructor(runtimeException);
        rteConstructor.builder()
                .setReturnType(runtime.parameterizedTypeReturnTypeOfConstructor())
                .commitParameters().commit();
        runtimeException.builder().setTypeNature(typeNatureClass)
                .setParentClass(runtime.newParameterizedType(exception, 0))
                .addConstructor(rteConstructor);

        MethodInfo valueOf = runtime.newMethod(runtime.boxedLongTypeInfo(), "valueOf", runtime.methodTypeStaticMethod());
        valueOf.builder()
                .setReturnType(runtime.newParameterizedType(runtime.boxedLongTypeInfo(), 0))
                .addParameter("l", runtime.longParameterizedType());
        valueOf.builder().commitParameters().commit();
        runtime.boxedLongTypeInfo().builder().addMethod(valueOf);

        MethodInfo length = runtime.newMethod(runtime.stringTypeInfo(), "length", runtime.methodTypeMethod());
        length.builder().setReturnType(runtime.intParameterizedType());
        length.builder().commit();
        runtime.stringTypeInfo().builder().addMethod(length);

        MethodInfo close = runtime.newMethod(autoCloseable, "close", runtime.methodTypeAbstractMethod());
        close.builder().setReturnType(runtime.voidParameterizedType()).commit();
        autoCloseable.builder().addMethod(close).setParentClass(runtime.objectParameterizedType());

        TypeParameter htTpK = runtime.newTypeParameter(0, "K", hashtable);
        TypeParameter htTpV = runtime.newTypeParameter(1, "V", hashtable);
        ParameterizedType htTpKPt = runtime.newParameterizedType(htTpK, 0, null);
        ParameterizedType htTpVPt = runtime.newParameterizedType(htTpV, 0, null);
        hashtable.builder()
                .setParentClass(runtime.objectParameterizedType())
                .addOrSetTypeParameter(htTpK)
                .addOrSetTypeParameter(htTpV);
        MethodInfo htConstructor = runtime.newConstructor(hashtable);
        htConstructor.builder()
                .setReturnType(hashtable.asParameterizedType())
                .setAccess(runtime.accessPublic()).commit();
        hashtable.builder().addConstructor(htConstructor);
        MethodInfo htPut = runtime.newMethod(hashtable, "put", runtime.methodTypeAbstractMethod());
        htPut.builder().setReturnType(htTpVPt)
                .addParameter("k", htTpKPt);
        htPut.builder().setAccess(runtime.accessPublic()).commitParameters().commit();
        hashtable.builder().addMethod(htPut);
        MethodInfo htGet = runtime.newMethod(hashtable, "get", runtime.methodTypeAbstractMethod());
        htGet.builder()
                .setReturnType(htTpVPt)
                .addParameter("k", htTpKPt);
        htGet.builder().setAccess(runtime.accessPublic()).commitParameters().commit();
        hashtable.builder().addMethod(htGet);
        hashtable.builder().setTypeNature(typeNatureClass).commit();

        annotationClass.builder().setTypeNature(typeNatureClass).setParentClass(runtime.objectParameterizedType()).commit();
    }

    private void defineFunction() {
        TypeParameter T = runtime.newTypeParameter(0, "T", function);
        T.builder().commit();
        TypeParameter R = runtime.newTypeParameter(1, "R", function);
        R.builder().commit();
        function.builder().addOrSetTypeParameter(T).addOrSetTypeParameter(R).setTypeNature(runtime.typeNatureInterface());
        MethodInfo apply = runtime.newMethod(function, "apply", runtime.methodTypeAbstractMethod());
        apply.builder().setReturnType(runtime.newParameterizedType(R, 0, null))
                .addMethodModifier(runtime.methodModifierPublic())
                .addParameter("t", runtime.newParameterizedType(T, 0, null));
        apply.builder().computeAccess();
        apply.builder().commit();
        function.builder()
                .setParentClass(runtime.objectParameterizedType())
                .addMethod(apply).addTypeModifier(runtime.typeModifierPublic())
                .setSingleAbstractMethod(apply)
                .computeAccess()
                .hierarchyIsDone()
                .commit();
    }

    private void defineConsumer() {
        TypeParameter T = runtime.newTypeParameter(0, "T", consumer);
        T.builder().commit();
        consumer.builder().addOrSetTypeParameter(T).setTypeNature(runtime.typeNatureInterface());
        MethodInfo accept = runtime.newMethod(consumer, "accept", runtime.methodTypeAbstractMethod());
        accept.builder().setReturnType(runtime.voidParameterizedType())
                .addMethodModifier(runtime.methodModifierPublic())
                .addParameter("t", runtime.newParameterizedType(T, 0, null));
        accept.builder().computeAccess();
        accept.builder().commit();
        consumer.builder().addMethod(accept).addTypeModifier(runtime.typeModifierPublic())
                .setSingleAbstractMethod(accept)
                .computeAccess()
                .setParentClass(runtime.objectParameterizedType())
                .hierarchyIsDone()
                .commit();
    }

    private void defineBiConsumer() {
        TypeParameter T = runtime.newTypeParameter(0, "T", biConsumer);
        T.builder().commit();
        TypeParameter U = runtime.newTypeParameter(1, "U", biConsumer);
        U.builder().commit();
        biConsumer.builder().addOrSetTypeParameter(T).addOrSetTypeParameter(U).setTypeNature(runtime.typeNatureInterface());
        MethodInfo accept = runtime.newMethod(biConsumer, "accept", runtime.methodTypeAbstractMethod());
        accept.builder().setReturnType(runtime.voidParameterizedType())
                .addMethodModifier(runtime.methodModifierPublic())
                .addParameter("t", runtime.newParameterizedType(T, 0, null));
        accept.builder().addParameter("u", runtime.newParameterizedType(U, 0, null));
        accept.builder().computeAccess();
        accept.builder().commit();
        biConsumer.builder().addMethod(accept).addTypeModifier(runtime.typeModifierPublic())
                .setSingleAbstractMethod(accept)
                .computeAccess()
                .setParentClass(runtime.objectParameterizedType())
                .hierarchyIsDone()
                .commit();
    }


    protected record ParseResult(Context context, List<TypeInfo> types) {
    }

    protected Context parseReturnContext(String input) {
        return parseReturnBoth(input, false, false).context;
    }

    protected TypeInfo parse(String input) {
        return parse(input, false);
    }

    protected TypeInfo parse(String input, boolean detailedSources) {
        return parseReturnBoth(input, true, detailedSources).types.getFirst();
    }

    protected ParseResult parseReturnBoth(String input, boolean failFast, boolean detailedSources) {
        Summary failFastSummary = new SummaryImpl(failFast);
        Supplier<JavaParser> parser = () -> {
            JavaParser p = new JavaParser(input);
            p.setParserTolerant(false);
            return p;
        };
        Map<String, TypeInfo> typeMap = new HashMap<>();
        CompiledTypesManager compiledTypesManager = new CompiledTypesManagerImpl(typeMap);
        TypeContextImpl typeContext = new TypeContextImpl(runtime, compiledTypesManager, false);
        Resolver resolver = new ResolverImpl(runtime.computeMethodOverrides(), new ParseHelperImpl(runtime), false);
        Context rootContext = ContextImpl.create(runtime, compiledTypesManager, failFastSummary, resolver, typeContext,
                detailedSources, false);
        SourceSet sourceSet = new SourceSetImpl("main", List.of(), URI.create("file:/"), StandardCharsets.UTF_8,
                false, false, false, false, false, Set.of(), Set.of());
        ScanCompilationUnit scanCompilationUnit = new ScanCompilationUnit(failFastSummary, runtime);
        CompilationUnit cu;
        try {
            ScanCompilationUnit.ScanResult sr = scanCompilationUnit.scan(new URI("input"),
                    sourceSet, null, parser.get().CompilationUnit(), detailedSources);
            typeMap.putAll(sr.sourceTypes());
            cu = sr.compilationUnit();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        ParseCompilationUnit parseCompilationUnit = new ParseCompilationUnit(rootContext);
        List<Either<TypeInfo, ParseTypeDeclaration.DelayedParsingInformation>> types
                = parseCompilationUnit.parse(cu, parser.get().CompilationUnit());
        computeSingleAbstractMethods(types.stream().map(Either::getLeft).toList());
        rootContext.resolver().resolve(true);
        return new ParseResult(rootContext, types.stream().map(Either::getLeft).toList());
    }

    private void computeSingleAbstractMethods(Collection<TypeInfo> types) {
        ComputeMethodOverrides cmo = runtime.computeMethodOverrides();
        types.parallelStream().flatMap(TypeInfo::recursiveSubTypeStream).forEach(ti -> {
            if (!ti.hasBeenInspected()) {
                MethodInfo sam = cmo.computeFunctionalInterface(ti);
                ti.builder().setSingleAbstractMethod(sam);
            } // else: package-info
        });
    }
}
