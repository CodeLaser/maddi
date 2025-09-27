/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.bytecode.java.asm;

import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.element.FingerPrint;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.resource.ByteCodeInspector;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.MD5FingerPrint;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/*
The ByteCodeInspectorImpl is used as a singleton.
Its access is protected in CompiledTypesManager
*/
public class ByteCodeInspectorImpl implements ByteCodeInspector, LocalTypeMap {
    private static final Logger LOGGER = LoggerFactory.getLogger(ByteCodeInspectorImpl.class);

    private enum Status {
        BEING_LOADED, DONE, IN_QUEUE, ON_DEMAND
    }

    private record TypeData(TypeInfo typeInfo,
                            Status status,
                            TypeParameterContext typeParameterContext) {
    }

    private final ReentrantReadWriteLock localTypeMapLock = new ReentrantReadWriteLock();
    private final Map<String, TypeData> localTypeMap = new LinkedHashMap<>();
    private final Runtime runtime;
    private final CompiledTypesManager compiledTypesManager;
    private final MessageDigest md;
    private final boolean allowCreationOfStubTypes;

    public ByteCodeInspectorImpl(Runtime runtime,
                                 CompiledTypesManager compiledTypesManager,
                                 boolean computeFingerPrints,
                                 boolean allowCreationOfStubTypes) {
        this.runtime = runtime;
        this.compiledTypesManager = compiledTypesManager;
        this.allowCreationOfStubTypes = allowCreationOfStubTypes;
        for (TypeInfo ti : runtime.predefinedObjects()) {
            localTypeMap.put(ti.fullyQualifiedName(),
                    new TypeData(ti, Status.IN_QUEUE, new TypeParameterContext()));
        }
        if (computeFingerPrints) {
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        } else {
            md = null;
        }
    }

    @Override
    public boolean acceptFQN(String fqName) {
        return compiledTypesManager.acceptFQN(fqName);
    }

    @Override
    public TypeInfo getLocal(String fqName) {
        TypeData typeData = localTypeMapGet(fqName);
        return typeData == null ? null : typeData.typeInfo;
    }

    @Override
    public String pathToFqn(String name) {
        return compiledTypesManager.classPath().pathToFqn(name);
    }

    private TypeData localTypeMapGet(String fqn) {
        localTypeMapLock.readLock().lock();
        try {
            return localTypeMap.get(fqn);
        } finally {
            localTypeMapLock.readLock().unlock();
        }
    }

    @Override
    public TypeInfo getOrCreate(String fqn, LoadMode loadMode) {
        if (!compiledTypesManager.acceptFQN(fqn)) {
            return null;
        }
        TypeData local = localTypeMapGet(fqn);
        TypeInfo typeInfo;
        TypeParameterContext typeParameterContext;
        if (local != null) {
            if (local.status == Status.DONE || local.status == Status.BEING_LOADED) {
                return local.typeInfo;
            }
            typeInfo = local.typeInfo;
            typeParameterContext = local.typeParameterContext;
        } else {
            typeInfo = null;
            typeParameterContext = new TypeParameterContext();
        }
        SourceFile source = typeInfo == null
                ? compiledTypesManager.classPath().fqnToPath(fqn, ".class")
                : compiledTypesManager.classPath().sourceFileOfType(typeInfo, ".class");
        if (source == null) {
            // we'll fail in other locations, but sometimes, we'll let it pass
            return null;
        }
        return inspectFromPath(typeInfo, source, typeParameterContext, loadMode);
    }

    @Override
    public TypeInfo load(SourceFile sourceFile, TypeInfo knownType) {
        String fqn = knownType.fullyQualifiedName();
        TypeData local = localTypeMapGet(fqn);
        TypeInfo typeInfo;
        TypeParameterContext typeParameterContext;
        if (local != null) {
            if (local.status == Status.DONE || local.status == Status.BEING_LOADED) {
                return local.typeInfo;
            }
            typeInfo = local.typeInfo;
            typeParameterContext = local.typeParameterContext;
        } else {
            typeInfo = knownType;
            typeParameterContext = new TypeParameterContext();
        }
        assert sourceFile != null;
        return inspectFromPath(typeInfo, sourceFile, typeParameterContext, LoadMode.NOW);
    }

    @Override
    public TypeInfo load(SourceFile sourceFile) {
        return inspectFromPath(null, sourceFile, new TypeParameterContext(), LoadMode.NOW);
    }

    @Override
    public TypeInfo inspectFromPath(TypeInfo typeInfoOrNull,
                                    SourceFile path,
                                    TypeParameterContext typeParameterContext,
                                    LoadMode loadMode) {
        assert path != null && path.path().endsWith(".class");
        String fqn;
        if (typeInfoOrNull != null) fqn = typeInfoOrNull.fullyQualifiedName();
        else fqn = pathToFqn(path.stripDotClass());
        TypeData td = localTypeMapGet(fqn);
        if (td != null && (td.status == Status.DONE || td.status == Status.BEING_LOADED)) {
            return td.typeInfo; // already working on it
        }
        TypeInfo typeInfo;
        if (td == null) {
            // may trigger recursion
            typeInfo = typeInfoOrNull != null ? typeInfoOrNull : createTypeInfo(path, fqn, typeParameterContext, loadMode);
        } else {
            typeInfo = td.typeInfo;
            assert typeInfoOrNull == null || typeInfoOrNull == typeInfo;
            if (typeInfo.compilationUnitOrEnclosingType().isRight()) {
                // may trigger recursion
                getOrCreate(typeInfo.compilationUnitOrEnclosingType().getRight().fullyQualifiedName(), loadMode);
            }
        }
        // because both the above if and else clause can trigger recursion, we must check again
        TypeData inMapAgain = localTypeMapGet(fqn);
        if (inMapAgain != null && (inMapAgain.status == Status.DONE || inMapAgain.status == Status.BEING_LOADED)) {
            return inMapAgain.typeInfo;
        }
        // jump to the typeInfo object in inMapAgain
        TypeInfo typeInfo1 = inMapAgain != null ? inMapAgain.typeInfo : typeInfo;
        if (loadMode == LoadMode.NOW) {
            return continueLoadByteCodeAndStartASM(path, fqn, typeInfo1, typeParameterContext);
        }
        Status newStatus = loadMode == LoadMode.QUEUE ? Status.IN_QUEUE : Status.ON_DEMAND;
        if (td == null || newStatus != td.status) {
            localTypeMapPut(fqn, new TypeData(typeInfo1, newStatus, new TypeParameterContext()));
        }
        if (!typeInfo1.haveOnDemandInspection()) {
            typeInfo1.setOnDemandInspection(ti -> {
                synchronized (ByteCodeInspectorImpl.this) {
                    inspectFromPath(ti, path, typeParameterContext, LoadMode.NOW);
                }
            });
        }
        return typeInfo1;
    }

    private TypeData localTypeMapPut(String fqn, TypeData typeData) {
        localTypeMapLock.writeLock().lock();
        try {
            return localTypeMap.put(fqn, typeData);
        } finally {
            localTypeMapLock.writeLock().unlock();
        }
    }

    private TypeInfo createTypeInfo(SourceFile source,
                                    String fqn,
                                    TypeParameterContext typeParameterContext,
                                    LoadMode loadMode) {
        String path = source.stripDotClass();
        int dollar = path.lastIndexOf('$');
        TypeInfo typeInfo;
        if (dollar >= 0) {
            String simpleName = path.substring(dollar + 1);
            String newPathWithoutSubType = SourceFile.ensureDotClass(path.substring(0, dollar));
            SourceFile newSource = source.withPath(newPathWithoutSubType);
            TypeInfo parent = inspectFromPath(null, newSource, typeParameterContext, loadMode);
            typeInfo = runtime.newTypeInfo(parent, simpleName);
        } else {
            int lastDot = fqn.lastIndexOf(".");
            String packageName = fqn.substring(0, lastDot);
            String simpleName = fqn.substring(lastDot + 1);
            CompilationUnit cu = runtime.newCompilationUnitBuilder()
                    .setURI(source.uri())
                    .setPackageName(packageName)
                    .setSourceSet(source.sourceSet())
                    .setFingerPrint(source.fingerPrint())
                    .build();
            typeInfo = runtime.newTypeInfo(cu, simpleName);
        }
        return typeInfo;
    }

    private TypeInfo continueLoadByteCodeAndStartASM(SourceFile path,
                                                     String fqn,
                                                     TypeInfo typeInfo,
                                                     TypeParameterContext typeParameterContext) {
        TypeData prev = localTypeMapPut(fqn, new TypeData(typeInfo, Status.BEING_LOADED, typeParameterContext));
        assert prev == null || prev.status != Status.DONE;
        try {
            byte[] classBytes = compiledTypesManager.classPath().loadBytes(path.path());
            if (classBytes == null) {
                return null;
            }
            // NOTE: the fingerprint null check is there for java.lang.String and the boxed types.
            if (typeInfo.isPrimaryType() && typeInfo.compilationUnit().fingerPrintOrNull() == null) {
                FingerPrint fingerPrint = makeFingerPrint(classBytes);
                typeInfo.compilationUnit().setFingerPrint(fingerPrint);
            }
            ClassReader classReader = new ClassReader(classBytes);
            LOGGER.debug("Constructed class reader for {} with {} bytes", fqn, classBytes.length);

            MyClassVisitor myClassVisitor = new MyClassVisitor(runtime, typeInfo, this,
                    typeParameterContext, path);
            classReader.accept(myClassVisitor, 0);
            LOGGER.debug("Finished bytecode inspection of {}", fqn);
            localTypeMapPut(fqn, new TypeData(typeInfo, Status.DONE, typeParameterContext));
            return typeInfo;
        } catch (RuntimeException | AssertionError re) {
            LOGGER.error("Path = {}", path);
            LOGGER.error("FQN  = {}", fqn);
            LOGGER.error("Number of compiled types = {}", compiledTypesManager.typesLoaded(true).size());
            throw re;
        }
    }

    private FingerPrint makeFingerPrint(byte[] classBytes) {
        if (md == null) return MD5FingerPrint.NO_FINGERPRINT;
        synchronized (md) {
            return MD5FingerPrint.compute(md, classBytes);
        }
    }

    @Override
    public boolean allowCreationOfStubTypes() {
        return allowCreationOfStubTypes;
    }
}

