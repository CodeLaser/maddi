package io.codelaser.maddi.modification.analyzer.modification;

import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.modification.analyzer.CommonTest;
import io.codelaser.maddi.modification.prepwork.variable.VariableData;
import io.codelaser.maddi.modification.prepwork.variable.VariableInfo;
import io.codelaser.maddi.modification.prepwork.variable.impl.VariableDataImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code instance.setKind(command.getKind())}: the setter keeps its argument, it does not modify it, so neither the
 * value handed over nor the object it was read from is modified -- whatever the value's type. Found on
 * dolphinscheduler (an enum read from a command and set on a workflow instance): the command was reported modified.
 */
public class TestSetterArgumentNotModified extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.List;
            class X {
                enum Kind { A, B }
                static class Command {
                    private Kind kind;
                    private String name;
                    private List<String> tags;
                    Kind getKind() { return kind; }
                    String getName() { return name; }
                    List<String> getTags() { return tags; }
                    void setKind(Kind kind) { this.kind = kind; }
                }
                static class Instance {
                    private Kind kind;
                    private String name;
                    private List<String> tags;
                    void setKind(Kind kind) { this.kind = kind; }
                    void setName(String name) { this.name = name; }
                    void setTags(List<String> tags) { this.tags = tags; }
                }
                void enumShared(Command command, Instance instance) {
                    instance.setKind(command.getKind());
                }
                void stringShared(Command command, Instance instance) {
                    instance.setName(command.getName());
                }
                void listShared(Command command, Instance instance) {
                    instance.setTags(command.getTags());
                }
            }
            """;

    private static String state(MethodInfo m) {
        ParameterInfo command = m.parameters().getFirst();
        VariableData vd = VariableDataImpl.of(m.methodBody().statements().getLast());
        VariableInfo vi = vd.variableInfo(command);
        return m.name() + ": command modified=" + command.isModified() + " vd=" + vi.isModified() + " links="
               + vi.linkedVariables() + " mlv=" + m.analysis().getOrNull(METHOD_LINKS,
                io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl.class);
    }

    @DisplayName("a setter's argument, and the object it was read from, are not modified")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);

        TypeInfo instance = X.findSubType("Instance");
        for (String setter : List.of("setKind", "setName")) {
            ParameterInfo p = instance.findUniqueMethod(setter, 1).parameters().getFirst();
            assertFalse(p.isModified(), setter + "'s parameter is only stored");
        }
        MethodInfo enumShared = X.findUniqueMethod("enumShared", 2);
        MethodInfo stringShared = X.findUniqueMethod("stringShared", 2);
        MethodInfo listShared = X.findUniqueMethod("listShared", 2);
        assertFalse(enumShared.parameters().getFirst().isModified(), state(enumShared));
        assertFalse(stringShared.parameters().getFirst().isModified(), state(stringShared));
        // a List is shared: the setter's modification of instance may be of it -- the over-approximation stays
        assertTrue(listShared.parameters().getFirst().isModified(), state(listShared));
        // the instance is modified in every case: its setter writes its own field
        assertTrue(enumShared.parameters().get(1).isModified());
    }
}
