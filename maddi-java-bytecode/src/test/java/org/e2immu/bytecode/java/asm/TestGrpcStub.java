/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
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

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.inspection.api.resource.ByteCodeInspector;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.Resources;
import org.e2immu.language.inspection.api.resource.SourceFile;
import org.e2immu.language.inspection.integration.CompiledTypesManagerImpl;
import org.e2immu.language.inspection.integration.ResourcesImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestGrpcStub {

    @Test
    public void test() throws IOException, URISyntaxException {
        Resources cp = new ResourcesImpl(Path.of("."));
        SourceSet base = addJmod("java.base", cp);
        SourceSet logging = addJmod("java.logging", cp);
        SourceSet guava = addJar("guava-33.3.0-jre", cp, List.of(base, logging));
        SourceSet api = addJar("grpc-api-1.67.1", cp, List.of(base, logging));
        SourceSet set67 = addJar("grpc-stub-1.67.1", cp, List.of(base, logging, guava, api));
        set67.computePriorityDependencies();
        SourceSet set73 = addJar("grpc-stub-1.73.0", cp, List.of(base, logging, guava, api));
        set73.computePriorityDependencies();

        CompiledTypesManagerImpl ctm = new CompiledTypesManagerImpl(base, cp);
        Runtime runtime = new RuntimeImpl();
        ByteCodeInspector byteCodeInspector = new ByteCodeInspectorImpl(runtime, ctm, true,
                true);
        ctm.setByteCodeInspector(byteCodeInspector);
        ctm.addToTrie(cp, true);
        ctm.preload("java.lang");

        List<SourceFile> sourceFiles = ctm.sourceFiles("io/grpc/stub/ClientCalls");
        assertEquals(2, sourceFiles.size());
        assertEquals(set67, sourceFiles.getFirst().sourceSet());
        assertEquals(set73, sourceFiles.getLast().sourceSet());

        SourceSet want67 = new SourceSetImpl.Builder()
                .setName("want67")
                .setUri(URI.create("file:/"))
                .setTest(true)
                .setDependencies(List.of(set67))
                .build();
        want67.computePriorityDependencies();

        TypeInfo clientCalls67 = ctm.getOrLoad("io.grpc.stub.ClientCalls", want67);
        assertEquals(16, clientCalls67.methods().size());
        assertEquals("""
                io.grpc.stub.ClientCalls.asyncUnaryCall(io.grpc.ClientCall,Object,io.grpc.stub.StreamObserver)
                io.grpc.stub.ClientCalls.asyncServerStreamingCall(io.grpc.ClientCall,Object,io.grpc.stub.StreamObserver)
                io.grpc.stub.ClientCalls.asyncClientStreamingCall(io.grpc.ClientCall,io.grpc.stub.StreamObserver)
                io.grpc.stub.ClientCalls.asyncBidiStreamingCall(io.grpc.ClientCall,io.grpc.stub.StreamObserver)
                io.grpc.stub.ClientCalls.blockingUnaryCall(io.grpc.ClientCall,Object)
                io.grpc.stub.ClientCalls.blockingUnaryCall(io.grpc.Channel,io.grpc.MethodDescriptor,io.grpc.CallOptions,Object)
                io.grpc.stub.ClientCalls.blockingServerStreamingCall(io.grpc.ClientCall,Object)
                io.grpc.stub.ClientCalls.blockingServerStreamingCall(io.grpc.Channel,io.grpc.MethodDescriptor,io.grpc.CallOptions,Object)
                io.grpc.stub.ClientCalls.futureUnaryCall(io.grpc.ClientCall,Object)
                io.grpc.stub.ClientCalls.getUnchecked(java.util.concurrent.Future)
                io.grpc.stub.ClientCalls.toStatusRuntimeException(Throwable)
                io.grpc.stub.ClientCalls.cancelThrow(io.grpc.ClientCall,Throwable)
                io.grpc.stub.ClientCalls.asyncUnaryRequestCall(io.grpc.ClientCall,Object,io.grpc.stub.StreamObserver,boolean)
                io.grpc.stub.ClientCalls.asyncUnaryRequestCall(io.grpc.ClientCall,Object,io.grpc.stub.ClientCalls.StartableListener)
                io.grpc.stub.ClientCalls.asyncStreamingRequestCall(io.grpc.ClientCall,io.grpc.stub.StreamObserver,boolean)
                io.grpc.stub.ClientCalls.startCall(io.grpc.ClientCall,io.grpc.stub.ClientCalls.StartableListener)\
                """, clientCalls67.methods().stream().map(MethodInfo::fullyQualifiedName).collect(Collectors.joining("\n")));
        assertEquals(set67, clientCalls67.compilationUnit().sourceSet());

        SourceSet want73 = new SourceSetImpl.Builder()
                .setName("want73")
                .setUri(URI.create("file:/"))
                .setTest(true)
                .setDependencies(List.of(set73))
                .build();
        want73.computePriorityDependencies();

        TypeInfo clientCalls73 = ctm.getOrLoad("io.grpc.stub.ClientCalls", want73);
        assertNotEquals(clientCalls67, clientCalls73);

        assertEquals(19, clientCalls73.methods().size());
        assertEquals("""
                io.grpc.stub.ClientCalls.asyncUnaryCall(io.grpc.ClientCall,Object,io.grpc.stub.StreamObserver)
                io.grpc.stub.ClientCalls.asyncServerStreamingCall(io.grpc.ClientCall,Object,io.grpc.stub.StreamObserver)
                io.grpc.stub.ClientCalls.asyncClientStreamingCall(io.grpc.ClientCall,io.grpc.stub.StreamObserver)
                io.grpc.stub.ClientCalls.asyncBidiStreamingCall(io.grpc.ClientCall,io.grpc.stub.StreamObserver)
                io.grpc.stub.ClientCalls.blockingUnaryCall(io.grpc.ClientCall,Object)
                io.grpc.stub.ClientCalls.blockingUnaryCall(io.grpc.Channel,io.grpc.MethodDescriptor,io.grpc.CallOptions,Object)
                io.grpc.stub.ClientCalls.blockingServerStreamingCall(io.grpc.ClientCall,Object)
                io.grpc.stub.ClientCalls.blockingServerStreamingCall(io.grpc.Channel,io.grpc.MethodDescriptor,io.grpc.CallOptions,Object)
                io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(io.grpc.Channel,io.grpc.MethodDescriptor,io.grpc.CallOptions,Object)
                io.grpc.stub.ClientCalls.blockingClientStreamingCall(io.grpc.Channel,io.grpc.MethodDescriptor,io.grpc.CallOptions)
                io.grpc.stub.ClientCalls.blockingBidiStreamingCall(io.grpc.Channel,io.grpc.MethodDescriptor,io.grpc.CallOptions)
                io.grpc.stub.ClientCalls.futureUnaryCall(io.grpc.ClientCall,Object)
                io.grpc.stub.ClientCalls.getUnchecked(java.util.concurrent.Future)
                io.grpc.stub.ClientCalls.toStatusRuntimeException(Throwable)
                io.grpc.stub.ClientCalls.cancelThrow(io.grpc.ClientCall,Throwable)
                io.grpc.stub.ClientCalls.asyncUnaryRequestCall(io.grpc.ClientCall,Object,io.grpc.stub.StreamObserver,boolean)
                io.grpc.stub.ClientCalls.asyncUnaryRequestCall(io.grpc.ClientCall,Object,io.grpc.stub.ClientCalls.StartableListener)
                io.grpc.stub.ClientCalls.asyncStreamingRequestCall(io.grpc.ClientCall,io.grpc.stub.StreamObserver,boolean)
                io.grpc.stub.ClientCalls.startCall(io.grpc.ClientCall,io.grpc.stub.ClientCalls.StartableListener)\
                """, clientCalls73.methods().stream().map(MethodInfo::fullyQualifiedName).collect(Collectors.joining("\n")));
        assertEquals(set73, clientCalls73.compilationUnit().sourceSet());

        // now test some of the dependent types

        // here there's only one
        List<CompiledTypesManager.TypeData> clientCallData = ctm.typeDataList("io.grpc.ClientCall");
        assertEquals(1, clientCallData.size());
        assertSame(api, clientCallData.getFirst().sourceFile().sourceSet());

        // but here there are two
        List<CompiledTypesManager.TypeData> streamObserver = ctm.typeDataList("io.grpc.stub.StreamObserver");
        assertEquals(2, streamObserver.size());
        CompiledTypesManager.TypeData so67 = streamObserver.stream()
                .filter(td -> td.sourceFile().sourceSet() == set67).findFirst().orElseThrow();
        CompiledTypesManager.TypeData so73 = streamObserver.stream()
                .filter(td -> td.sourceFile().sourceSet() == set73).findFirst().orElseThrow();
        assertNotEquals(so67.typeInfo(), so73.typeInfo());
    }

    private static SourceSet addJmod(String name, Resources cp) throws URISyntaxException, IOException {
        URL url = ResourcesImpl.constructJModURL(name, null);
        SourceSet jmodBase = new SourceSetImpl.Builder()
                .setName("jmod:" + name)// TODO should we not remove "jmod:" ??
                .setUri(url.toURI())
                .setLibrary(true)
                .setExternalLibrary(true)
                .setPartOfJdk(true)
                .setModule(true)
                .build();
        cp.addJmod(new SourceFile("jmod:" + name, url.toURI(), jmodBase, null));
        jmodBase.computePriorityDependencies();
        return jmodBase;
    }

    private static SourceSet addJar(String name, Resources cp, List<SourceSet> dependencies) throws IOException {
        String path = "src/test/resources/" + name + ".jar";
        URI uri = URI.create("jar:file:" + path + "!/");
        SourceSet sourceSet = new SourceSetImpl.Builder()
                .setName(name)
                .setUri(uri)
                .setLibrary(true)
                .setExternalLibrary(true)
                .setDependencies(dependencies)
                .build();
        cp.addJar(new SourceFile(path, uri, sourceSet, null));
        sourceSet.computePriorityDependencies();
        return sourceSet;
    }
}
