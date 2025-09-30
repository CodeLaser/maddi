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
import org.e2immu.language.inspection.resource.CompiledTypesManagerImpl;
import org.e2immu.language.inspection.resource.ResourcesImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
        SourceSet guava = addJar("guava-33.3.0-jre", cp, Set.of(base, logging));
        SourceSet api = addJar("grpc-api-1.67.1", cp, Set.of(base, logging));
        SourceSet set67 = addJar("grpc-stub-1.67.1", cp, Set.of(base, logging, guava, api));
        set67.computePriorityDependencies();
        SourceSet set73 = addJar("grpc-stub-1.73.0", cp, Set.of(base, logging, guava, api));
        set73.computePriorityDependencies();

        CompiledTypesManagerImpl ctm = new CompiledTypesManagerImpl(base, cp);
        Runtime runtime = new RuntimeImpl();
        ByteCodeInspector byteCodeInspector = new ByteCodeInspectorImpl(runtime, ctm, true,
                true);
        ctm.setByteCodeInspector(byteCodeInspector);
        ctm.addToTrie(cp, true);
        ctm.preload("java.lang", base);

        List<SourceFile> sourceFiles = ctm.sourceFiles("io/grpc/stub/ClientCalls");
        assertEquals(2, sourceFiles.size());
        assertEquals(set67, sourceFiles.getFirst().sourceSet());
        assertEquals(set73, sourceFiles.getLast().sourceSet());

        SourceSet want67 = new SourceSetImpl("want67", List.of(), URI.create("file:/"), StandardCharsets.UTF_8,
                true, false, false, false, false, Set.of(), Set.of(set67));
        want67.computePriorityDependencies();

        TypeInfo clientCalls67 = ctm.getOrLoad("io.grpc.stub.ClientCalls", want67);
        assertEquals(16, clientCalls67.methods().size());
        assertEquals("""
                io.grpc.stub.ClientCalls.asyncUnaryCall(io.grpc.ClientCall<ReqT,RespT>,ReqT,io.grpc.stub.StreamObserver<RespT>)
                io.grpc.stub.ClientCalls.asyncServerStreamingCall(io.grpc.ClientCall<ReqT,RespT>,ReqT,io.grpc.stub.StreamObserver<RespT>)
                io.grpc.stub.ClientCalls.asyncClientStreamingCall(io.grpc.ClientCall<ReqT,RespT>,io.grpc.stub.StreamObserver<RespT>)
                io.grpc.stub.ClientCalls.asyncBidiStreamingCall(io.grpc.ClientCall<ReqT,RespT>,io.grpc.stub.StreamObserver<RespT>)
                io.grpc.stub.ClientCalls.blockingUnaryCall(io.grpc.ClientCall<ReqT,RespT>,ReqT)
                io.grpc.stub.ClientCalls.blockingUnaryCall(io.grpc.Channel,io.grpc.MethodDescriptor<ReqT,RespT>,io.grpc.CallOptions,ReqT)
                io.grpc.stub.ClientCalls.blockingServerStreamingCall(io.grpc.ClientCall<ReqT,RespT>,ReqT)
                io.grpc.stub.ClientCalls.blockingServerStreamingCall(io.grpc.Channel,io.grpc.MethodDescriptor<ReqT,RespT>,io.grpc.CallOptions,ReqT)
                io.grpc.stub.ClientCalls.futureUnaryCall(io.grpc.ClientCall<ReqT,RespT>,ReqT)
                io.grpc.stub.ClientCalls.getUnchecked(java.util.concurrent.Future<V>)
                io.grpc.stub.ClientCalls.toStatusRuntimeException(Throwable)
                io.grpc.stub.ClientCalls.cancelThrow(io.grpc.ClientCall<?,?>,Throwable)
                io.grpc.stub.ClientCalls.asyncUnaryRequestCall(io.grpc.ClientCall<ReqT,RespT>,ReqT,io.grpc.stub.StreamObserver<RespT>,boolean)
                io.grpc.stub.ClientCalls.asyncUnaryRequestCall(io.grpc.ClientCall<ReqT,RespT>,ReqT,io.grpc.stub.ClientCalls.StartableListener<RespT>)
                io.grpc.stub.ClientCalls.asyncStreamingRequestCall(io.grpc.ClientCall<ReqT,RespT>,io.grpc.stub.StreamObserver<RespT>,boolean)
                io.grpc.stub.ClientCalls.startCall(io.grpc.ClientCall<ReqT,RespT>,io.grpc.stub.ClientCalls.StartableListener<RespT>)\
                """, clientCalls67.methods().stream().map(MethodInfo::fullyQualifiedName).collect(Collectors.joining("\n")));
        assertEquals(set67, clientCalls67.compilationUnit().sourceSet());

        SourceSet want73 = new SourceSetImpl("want73", List.of(), URI.create("file:/"), StandardCharsets.UTF_8,
                true, false, false, false, false, Set.of(), Set.of(set73));
        want73.computePriorityDependencies();

        TypeInfo clientCalls73 = ctm.getOrLoad("io.grpc.stub.ClientCalls", want73);
        assertNotEquals(clientCalls67, clientCalls73);

        assertEquals(19, clientCalls73.methods().size());
        assertEquals("""
                io.grpc.stub.ClientCalls.asyncUnaryCall(io.grpc.ClientCall<ReqT,RespT>,ReqT,io.grpc.stub.StreamObserver<RespT>)
                io.grpc.stub.ClientCalls.asyncServerStreamingCall(io.grpc.ClientCall<ReqT,RespT>,ReqT,io.grpc.stub.StreamObserver<RespT>)
                io.grpc.stub.ClientCalls.asyncClientStreamingCall(io.grpc.ClientCall<ReqT,RespT>,io.grpc.stub.StreamObserver<RespT>)
                io.grpc.stub.ClientCalls.asyncBidiStreamingCall(io.grpc.ClientCall<ReqT,RespT>,io.grpc.stub.StreamObserver<RespT>)
                io.grpc.stub.ClientCalls.blockingUnaryCall(io.grpc.ClientCall<ReqT,RespT>,ReqT)
                io.grpc.stub.ClientCalls.blockingUnaryCall(io.grpc.Channel,io.grpc.MethodDescriptor<ReqT,RespT>,io.grpc.CallOptions,ReqT)
                io.grpc.stub.ClientCalls.blockingServerStreamingCall(io.grpc.ClientCall<ReqT,RespT>,ReqT)
                io.grpc.stub.ClientCalls.blockingServerStreamingCall(io.grpc.Channel,io.grpc.MethodDescriptor<ReqT,RespT>,io.grpc.CallOptions,ReqT)
                io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(io.grpc.Channel,io.grpc.MethodDescriptor<ReqT,RespT>,io.grpc.CallOptions,ReqT)
                io.grpc.stub.ClientCalls.blockingClientStreamingCall(io.grpc.Channel,io.grpc.MethodDescriptor<ReqT,RespT>,io.grpc.CallOptions)
                io.grpc.stub.ClientCalls.blockingBidiStreamingCall(io.grpc.Channel,io.grpc.MethodDescriptor<ReqT,RespT>,io.grpc.CallOptions)
                io.grpc.stub.ClientCalls.futureUnaryCall(io.grpc.ClientCall<ReqT,RespT>,ReqT)
                io.grpc.stub.ClientCalls.getUnchecked(java.util.concurrent.Future<V>)
                io.grpc.stub.ClientCalls.toStatusRuntimeException(Throwable)
                io.grpc.stub.ClientCalls.cancelThrow(io.grpc.ClientCall<?,?>,Throwable)
                io.grpc.stub.ClientCalls.asyncUnaryRequestCall(io.grpc.ClientCall<ReqT,RespT>,ReqT,io.grpc.stub.StreamObserver<RespT>,boolean)
                io.grpc.stub.ClientCalls.asyncUnaryRequestCall(io.grpc.ClientCall<ReqT,RespT>,ReqT,io.grpc.stub.ClientCalls.StartableListener<RespT>)
                io.grpc.stub.ClientCalls.asyncStreamingRequestCall(io.grpc.ClientCall<ReqT,RespT>,io.grpc.stub.StreamObserver<RespT>,boolean)
                io.grpc.stub.ClientCalls.startCall(io.grpc.ClientCall<ReqT,RespT>,io.grpc.stub.ClientCalls.StartableListener<RespT>)\
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
        SourceSet jmodBase = new SourceSetImpl("jmod:" + name, List.of(), url.toURI(), StandardCharsets.UTF_8,
                false, true, true, true, false, Set.of(), Set.of());
        cp.addJmod(new SourceFile("jmod:" + name, url.toURI(), jmodBase, null));
        jmodBase.computePriorityDependencies();
        return jmodBase;
    }

    private static SourceSet addJar(String name, Resources cp, Set<SourceSet> dependencies) throws IOException {
        String path = "src/test/resources/" + name + ".jar";
        URI uri = URI.create("jar:file:" + path + "!/");
        SourceSet sourceSet = new SourceSetImpl(name, List.of(), uri,
                StandardCharsets.UTF_8, false, true, true, false, false,
                Set.of(), dependencies);
        cp.addJar(new SourceFile(path, uri, sourceSet, null));
        sourceSet.computePriorityDependencies();
        return sourceSet;
    }
}
