/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.bytecode.java.asm;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.inspection.api.resource.ByteCodeInspector;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.objectweb.asm.Opcodes.ASM9;

public class MyFieldVisitor extends FieldVisitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyFieldVisitor.class);

    private final FieldInfo fieldInfo;
    private final Runtime runtime;
    private final LocalTypeMap localTypeMap;
    private final ByteCodeInspector.TypeParameterContext typeContext;
    private final SourceSet sourceSetOfRequest;

    public MyFieldVisitor(Runtime runtime,
                          ByteCodeInspector.TypeParameterContext typeContext,
                          SourceSet sourceSetOfRequest,
                          FieldInfo fieldInfo,
                          LocalTypeMap localTypeMap) {
        super(ASM9);
        this.runtime = runtime;
        this.fieldInfo = fieldInfo;
        this.localTypeMap = localTypeMap;
        this.typeContext = typeContext;
        this.sourceSetOfRequest = sourceSetOfRequest;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        LOGGER.debug("Have field annotation {} {}", descriptor, visible);
        return new MyAnnotationVisitor<>(runtime, sourceSetOfRequest, fieldInfo.owner().compilationUnit().sourceSet(),
                typeContext, localTypeMap, descriptor, fieldInfo.builder());
    }

    @Override
    public void visitEnd() {
        fieldInfo.builder().computeAccess();
        fieldInfo.builder().commit();
        fieldInfo.owner().builder().addField(fieldInfo);
    }
}
