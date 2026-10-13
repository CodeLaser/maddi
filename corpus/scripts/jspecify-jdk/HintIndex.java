import com.sun.source.tree.*;
import com.sun.source.util.*;
import javax.tools.*;
import java.nio.file.*;
import java.util.*;

/**
 * Index of a maddi hint file: shadow types and members with source offsets, for the JSpecify nullness generator.
 * TYPE fqn bodyEnd | MEMBER fqn name erasedParams declStart methodAnn paramStarts paramAnns | OUTER end importsEnd
 */
public class HintIndex {
    static String pkg;
    static SourcePositions pos;
    static CompilationUnitTree cu;
    static List<String> out = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = javac.getStandardFileManager(null, null, null);
        for (int i = 0; i < args.length; i += 2) {
            out.clear();
            JavacTask task = (JavacTask) javac.getTask(null, fm, d -> {}, List.of("-proc:none"), null,
                    fm.getJavaFileObjects(Path.of(args[i])));
            cu = task.parse().iterator().next();
            pos = Trees.instance(task).getSourcePositions();
            long importsEnd = cu.getImports().isEmpty() ? pos.getEndPosition(cu, cu.getPackage())
                    : pos.getEndPosition(cu, cu.getImports().get(cu.getImports().size() - 1));
            for (ImportTree it : cu.getImports()) out.add("IMPORT\t" + it.getQualifiedIdentifier());
            ClassTree outer = (ClassTree) cu.getTypeDecls().get(0);
            for (Tree m : outer.getMembers()) {
                if (m instanceof VariableTree vt && vt.getName().contentEquals("PACKAGE_NAME")) {
                    pkg = vt.getInitializer().toString().replace("\"", "");
                }
            }
            out.add("OUTER\t" + (pos.getEndPosition(cu, outer) - 1) + "\t" + importsEnd + "\t" + pkg);
            for (Tree m : outer.getMembers()) {
                if (m instanceof ClassTree ct && ct.getSimpleName().toString().endsWith("$")) {
                    String name = ct.getSimpleName().toString();
                    visit(ct, pkg + "." + name.substring(0, name.length() - 1), new ArrayDeque<>());
                }
            }
            Files.write(Path.of(args[i + 1]), out);
        }
    }

    static void visit(ClassTree ct, String fqn, Deque<Map<String, String>> tvs) {
        out.add("TYPE\t" + fqn + "\t" + (pos.getEndPosition(cu, ct) - 1) + "\t" + pos.getStartPosition(cu, ct) + "\t"
                + (ct.getKind() == Tree.Kind.INTERFACE ? "interface" : "class"));
        Map<String, String> mine = new HashMap<>();
        for (TypeParameterTree tp : ct.getTypeParameters()) mine.put(tp.getName().toString(), bound(tp, tvs));
        tvs.push(mine);
        String simple = ct.getSimpleName().toString();
        for (Tree m : ct.getMembers()) {
            if (m instanceof ClassTree nested) {
                visit(nested, fqn + "." + nested.getSimpleName(), tvs);
            } else if (m instanceof MethodTree mt) {
                Map<String, String> mtv = new HashMap<>();
                for (TypeParameterTree tp : mt.getTypeParameters()) mtv.put(tp.getName().toString(), bound(tp, tvs));
                tvs.push(mtv);
                String name = mt.getReturnType() == null || mt.getName().contentEquals(simple) ? "<init>" : mt.getName().toString();
                List<String> types = new ArrayList<>(), starts = new ArrayList<>();
                StringBuilder anns = new StringBuilder();
                for (VariableTree p : mt.getParameters()) {
                    types.add(JSpecifyJdkDump.erase(p.getType(), tvs));
                    starts.add(String.valueOf(pos.getStartPosition(cu, p)));
                    anns.append(ann(p.getModifiers()));
                }
                out.add("MEMBER\t" + fqn + "\t" + name + "\t" + String.join(",", types) + "\t"
                        + pos.getStartPosition(cu, mt) + "\t" + ann(mt.getModifiers()) + "\t" + String.join(",", starts)
                        + "\t" + anns);
                tvs.pop();
            }
        }
        tvs.pop();
    }

    static String ann(ModifiersTree m) {
        if (JSpecifyJdkDump.has(m, "Nullable")) return "Q";
        if (JSpecifyJdkDump.has(m, "NotNull")) return "N";
        return "-";
    }

    static String bound(TypeParameterTree tp, Deque<Map<String, String>> tvs) {
        return tp.getBounds().isEmpty() ? "Object" : JSpecifyJdkDump.erase(tp.getBounds().get(0), tvs);
    }
}
