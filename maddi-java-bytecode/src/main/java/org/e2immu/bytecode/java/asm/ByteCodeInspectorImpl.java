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
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


/*
The ByteCodeInspectorImpl is used as a singleton.
Its access is protected in CompiledTypesManager
*/
public class ByteCodeInspectorImpl implements ByteCodeInspector, LocalTypeMap {
    private static final Logger LOGGER = LoggerFactory.getLogger(ByteCodeInspectorImpl.class);

    public record DataImpl(Status status,
                           TypeParameterContext typeParameterContext) implements ByteCodeInspector.Data {
        @Override
        public Data withParentTypeParameterContext(TypeParameterContext parent) {
            return new DataImpl(status, new TypeParameterContextImpl(parent));
        }
    }

    @Override
    public Data defaultData() {
        return new DataImpl(Status.ON_DEMAND, new TypeParameterContextImpl());
    }

    static class TypeParameterContextImpl implements ByteCodeInspector.TypeParameterContext {
        private final Map<String, TypeParameter> map = new HashMap<>();
        private final ByteCodeInspector.TypeParameterContext parent;

        public TypeParameterContextImpl() {
            this(null);
        }

        private TypeParameterContextImpl(ByteCodeInspector.TypeParameterContext parent) {
            this.parent = parent;
        }

        public void add(TypeParameter typeParameter) {
            map.put(typeParameter.simpleName(), typeParameter);
        }

        public TypeParameter get(String typeParamName) {
            TypeParameter here = map.get(typeParamName);
            if (here != null || parent == null) return here;
            return parent.get(typeParamName);
        }

        public ByteCodeInspector.TypeParameterContext newContext() {
            return new TypeParameterContextImpl(this);
        }
    }


    private final Runtime runtime;
    private final CompiledTypesManager compiledTypesManager;
    private final MessageDigest md;
    private final boolean allowCreationOfStubTypes;
    private final Map<String, Integer> duplicateWarnings = new ConcurrentHashMap<String, Integer>();

    public ByteCodeInspectorImpl(Runtime runtime,
                                 CompiledTypesManager compiledTypesManager,
                                 boolean computeFingerPrints,
                                 boolean allowCreationOfStubTypes) {
        this.runtime = runtime;
        this.compiledTypesManager = compiledTypesManager;
        this.allowCreationOfStubTypes = allowCreationOfStubTypes;
        // TODO should we add the predefined types???
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
    public CompiledTypesManager.TypeData typeData(String fqn, SourceSet sourceSetOfRequest, SourceSet nearestSourceSet) {
        return compiledTypesManager.typeDataOrNull(fqn, sourceSetOfRequest, nearestSourceSet, false);
    }

    @Override
    public String pathToFqn(String name) {
        return compiledTypesManager.classPath().pathToFqn(name);
    }

    @Override
    public TypeInfo getOrCreate(String fqn, SourceSet sourceSetOfRequest, SourceSet nearestSourceSet, LoadMode loadMode) {
        assert !fqn.contains("/");
        if (!compiledTypesManager.acceptFQN(fqn)) {
            return null;
        }
        CompiledTypesManager.TypeData typeData = typeData(fqn, sourceSetOfRequest, nearestSourceSet);
        if (typeData == null) {
            if (duplicateWarnings.merge(fqn, 1, Integer::sum) == 1) {
                LOGGER.warn("Not in classpath: {}, request from {}", fqn, sourceSetOfRequest.name());
            }
            return null;
        }
        TypeInfo typeInfo = typeData.typeInfo();
        if (typeInfo != null && !typeInfo.compilationUnit().externalLibrary()) {
            return typeInfo;
        }
        return inspectFromPath(typeData, sourceSetOfRequest, loadMode);
    }

    @Override
    public TypeInfo load(CompiledTypesManager.TypeData typeData, SourceSet sourceSetOfRequest) {
        return inspectFromPath(typeData, sourceSetOfRequest, LoadMode.NOW);
    }

    @Override
    public TypeInfo inspectFromPath(CompiledTypesManager.TypeData typeData, SourceSet sourceSetOfRequest,
                                    LoadMode loadMode) {
        Data data = typeData.byteCodeInspectorData();
        if (data == null) {
            LOGGER.warn("Not in classpath: {}, request from {}", typeData.sourceFile(), sourceSetOfRequest.name());
            return null;
        }
        if (typeData.typeInfo() != null) {
            if (data.status() == Status.BEING_LOADED || data.status() == Status.DONE) {
                return typeData.typeInfo();
            }
        } else {
            assert data.status() != Status.BEING_LOADED && data.status() != Status.DONE;
            String fqn = typeData.sourceFile().fullyQualifiedNameFromPath();
            TypeInfo typeInfo = createTypeInfo(typeData.sourceFile(), fqn, sourceSetOfRequest, loadMode);
            if (typeInfo == null) return null;// type missing
            if (typeData.typeInfo() == null) {
                // note: do not directly call setTypeInfo,
                compiledTypesManager.addTypeInfo(typeData.sourceFile(), typeInfo);
            } // else: already set in recursion
        }

        // because both the above if and else clause can trigger recursion, we must check again
        Data dataAgain = typeData.byteCodeInspectorData();
        if (dataAgain.status() == Status.DONE || dataAgain.status() == Status.BEING_LOADED) {
            return Objects.requireNonNull(typeData.typeInfo());
        }
        // jump to the typeInfo object in inMapAgain
        TypeInfo typeInfo1 = typeData.typeInfo();
        if (typeInfo1 == null) {
            LOGGER.warn("Ignoring {}, request from {}", typeData.sourceFile(), sourceSetOfRequest.name());
            return null;
        }
        if (loadMode == LoadMode.NOW) {
            return continueLoadByteCodeAndStartASM(typeData, sourceSetOfRequest, dataAgain.typeParameterContext());
        }
        Status newStatus = loadMode == LoadMode.QUEUE ? Status.IN_QUEUE : Status.ON_DEMAND;
        typeData.updateByteCodeInspectorData(new DataImpl(newStatus, new TypeParameterContextImpl()));
        if (!typeInfo1.haveOnDemandInspection()) {
            typeInfo1.setOnDemandInspection(_ -> {
                synchronized (ByteCodeInspectorImpl.this) {
                    inspectFromPath(typeData, sourceSetOfRequest, LoadMode.NOW);
                }
            });
        }
        return typeInfo1;
    }

    private TypeInfo createTypeInfo(SourceFile source, String fqn, SourceSet sourceSetOfRequest, LoadMode loadMode) {
        String path = source.stripDotClass();
        int dollar = path.lastIndexOf('$');
        TypeInfo typeInfo;
        if (dollar >= 0) {
            String simpleName = path.substring(dollar + 1);
            int lastDot = fqn.lastIndexOf('.');
            assert lastDot > 0;
            String enclosingFqn = fqn.substring(0, lastDot);
            TypeInfo enclosing = getOrCreate(enclosingFqn, sourceSetOfRequest, source.sourceSet(), loadMode);
            if (enclosing == null) {
                LOGGER.warn("Cannot find enclosing type {} of {}", enclosingFqn, fqn);
                return null;
            }
            // this may trigger the creation of the sub-type... so we must check
            TypeInfo alreadyCreated = enclosing.findSubType(simpleName, false);
            if (alreadyCreated != null) {
                typeInfo = alreadyCreated;
            } else {
                typeInfo = runtime.newTypeInfo(enclosing, simpleName);
            }
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

    private TypeInfo continueLoadByteCodeAndStartASM(CompiledTypesManager.TypeData typeData,
                                                     SourceSet sourceSetOfRequest,
                                                     TypeParameterContext typeParameterContext) {
        assert typeData.byteCodeInspectorData().status() != Status.DONE;
        typeData.updateByteCodeInspectorData(new DataImpl(Status.BEING_LOADED, typeParameterContext));
        try {
            byte[] classBytes = compiledTypesManager.classPath().loadBytes(typeData.sourceFile().uri());
            if (classBytes == null) {
                return null;
            }
            TypeInfo typeInfo = typeData.typeInfo();
            // NOTE: the fingerprint null check is there for java.lang.String and the boxed types.
            if (typeInfo.isPrimaryType() && typeInfo.compilationUnit().fingerPrintOrNull() == null) {
                FingerPrint fingerPrint = makeFingerPrint(classBytes);
                typeInfo.compilationUnit().setFingerPrint(fingerPrint);
            }
            ClassReader classReader = new ClassReader(classBytes);
            String fqn = typeInfo.fullyQualifiedName();
            LOGGER.debug("Constructed class reader for {} with {} bytes", fqn, classBytes.length);

            MyClassVisitor myClassVisitor = new MyClassVisitor(runtime, this, typeParameterContext, typeData,
                    sourceSetOfRequest);
            classReader.accept(myClassVisitor, 0);
            LOGGER.debug("Finished bytecode inspection of {}", fqn);
            typeData.updateByteCodeInspectorData(new DataImpl(Status.DONE, typeParameterContext));
            return typeInfo;
        } catch (RuntimeException | AssertionError re) {
            LOGGER.error("Path = {}", typeData.sourceFile());
            LOGGER.error("FQN  = {}", typeData.typeInfo().fullyQualifiedName());
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

