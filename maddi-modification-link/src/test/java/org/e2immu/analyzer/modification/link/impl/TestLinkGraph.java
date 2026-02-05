package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestLinkGraph extends CommonTest {

    // this.runtime.ctm.bci.t.runtime....


    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import org.e2immu.support.SetOnce;
            import java.util.ArrayList;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            
            class X {
                interface TypeInfo {
                    String fullyQualifiedName();
                 }
                interface Runtime {
                    List<TypeInfo> predefined();
                 }
            
                static class RuntimeImpl implements Runtime {
                    private CompiledTypesManager ctm;
                    RuntimeImpl(CompiledTypesManager ctm) {
                        this.ctm = ctm;
                    }
                    public List<TypeInfo> predefined() {
                        return new ArrayList<>();
                    }
                }
                interface ByteCodeInspector {
                    TypeInfo get(String fqn);
                }
                interface CompiledTypesManager {
                    void addPredefinedObjects(List<TypeInfo> typeInfos);
                    void setByteCodeInspector(ByteCodeInspector bc);
                    TypeInfo get(String fqn);
                }
                static class ByteCodeInspectorImpl implements ByteCodeInspector {
                    private final Runtime runtime;
                    private final CompiledTypesManager compiledTypesManager;
            
                    public ByteCodeInspectorImpl(Runtime runtime, CompiledTypesManager compiledTypesManager) {
                        this.runtime = runtime;
                        this.compiledTypesManager = compiledTypesManager;
                    }
                    public TypeInfo get(String fqn) {
                        return compiledTypesManager.get(fqn);
                    }
                }
                static class CompiledTypesManagerImpl implements CompiledTypesManager {
                    private final SetOnce<ByteCodeInspector> byteCodeInspector = new SetOnce<>();
                    private final Map<String, TypeInfo> mapSingleTypeForFQN = new HashMap<>();
            
                    public void setByteCodeInspector(ByteCodeInspector byteCodeInspector) {
                        this.byteCodeInspector.set(byteCodeInspector);
                    }
                    public void addPredefinedObjects(List<TypeInfo> typeInfos) {
                        for(TypeInfo typeInfo: typeInfos) {
                            this.mapSingleTypeForFQN.put(typeInfo.fullyQualifiedName(), get(typeInfo.fullyQualifiedName()));
                        }
                    }
                    public TypeInfo get(String fqn) {
                        return this.byteCodeInspector.get().get(fqn);
                    }
                }
                private Runtime runtime;
                private CompiledTypesManager ctm;
            
                void initialize(List<TypeInfo> typeInfos) {
                    CompiledTypesManagerImpl ctmi = new CompiledTypesManagerImpl();
            
                    runtime = new RuntimeImpl(ctm);
                    ByteCodeInspector bc = new ByteCodeInspectorImpl(runtime, ctmi);
                    ctm.setByteCodeInspector(bc);
                    ctm.addPredefinedObjects(runtime.predefined());
                }
             }
            """;

    //{DependentVariableImpl@6346}this.runtime.compiledTypesManager.byteCodeInspector.t.runtime.compiledTypesManager.byteCodeInspector.t.runtime.compiledTypesManager.byteCodeInspector.t.compiledTypesManager.mapSingleTypeForFQN.§$$s[-2] -> {HashMap@6347} size = 5
    @DisplayName("class reference cycles")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        tlc.doPrimaryType(X);
    }
}