import com.sun.source.tree.*;
import com.sun.source.util.*;
import javax.tools.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Parse-only dump of JSpecify's nullness for public JDK API: one line per method/constructor:
 * type FQN, name, erased simple param types, return state, param states, scope.
 * States: N non-null, Q nullable, P parametric (unannotated type variable), U unmarked, - primitive/void.
 * Then the nested positions that may be null, one level deep (a type argument annotated {@code @Nullable}, a
 * wildcard bounded by one, or an array's elements as position 0): of the return, and per parameter ({@code ;}-joined).
 */
public class JSpecifyJdkDump {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        List<Path> files;
        try (Stream<Path> s = Files.walk(root)) { files = s.filter(p -> p.toString().endsWith(".java")).toList(); }
        Set<String> markedPackages = new HashSet<>();
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = javac.getStandardFileManager(null, null, null);
        for (Path f : files) {
            if (!f.getFileName().toString().equals("package-info.java")) continue;
            if (Files.readString(f).contains("@NullMarked")) {
                markedPackages.add(root.relativize(f.getParent()).toString().replace('/', '.'));
            }
        }
        List<String> out = new ArrayList<>();
        for (Path f : files) {
            if (f.getFileName().toString().equals("package-info.java")) continue;
            JavacTask task = (JavacTask) javac.getTask(null, fm, d -> {}, List.of("-proc:none"), null,
                    fm.getJavaFileObjects(f));
            for (CompilationUnitTree cu : task.parse()) {
                String pkg = cu.getPackageName() == null ? "" : cu.getPackageName().toString();
                boolean pkgMarked = markedPackages.contains(pkg);
                for (Tree t : cu.getTypeDecls()) {
                    if (t instanceof ClassTree ct) visitClass(ct, pkg, pkgMarked, new ArrayDeque<>(), out);
                }
            }
        }
        Collections.sort(out);
        Files.write(Path.of(args[1]), out);
        System.err.println(out.size() + " members, marked packages " + markedPackages);
    }

    static boolean has(ModifiersTree m, String simple) {
        return m != null && m.getAnnotations().stream().anyMatch(a -> lastSeg(a.getAnnotationType().toString()).equals(simple));
    }

    static String lastSeg(String s) { return s.substring(s.lastIndexOf('.') + 1); }

    static boolean visible(ModifiersTree m, boolean inInterface) {
        Set<javax.lang.model.element.Modifier> f = m.getFlags();
        if (f.contains(javax.lang.model.element.Modifier.PRIVATE)) return false;
        return inInterface || f.contains(javax.lang.model.element.Modifier.PUBLIC)
               || f.contains(javax.lang.model.element.Modifier.PROTECTED);
    }

    // type variables in scope: name -> erased bound
    static void visitClass(ClassTree ct, String fqnPrefix, boolean marked, Deque<Map<String, String>> tvs, List<String> out) {
        boolean iface = ct.getKind() == Tree.Kind.INTERFACE || ct.getKind() == Tree.Kind.ANNOTATION_TYPE;
        if (!visible(ct.getModifiers(), false) && !fqnPrefix.contains("$IFACE")) {
            // nested types of interfaces are public implicitly; top-level non-public types are skipped
            if (!(ct.getModifiers().getFlags().isEmpty() && fqnPrefix.endsWith("#iface"))) return;
        }
        String fqn = (fqnPrefix.isEmpty() ? "" : fqnPrefix.replace("#iface", "") + ".") + ct.getSimpleName();
        if (has(ct.getModifiers(), "NullMarked")) marked = true;
        if (has(ct.getModifiers(), "NullUnmarked")) marked = false;
        Map<String, String> mine = new HashMap<>();
        for (TypeParameterTree tp : ct.getTypeParameters()) mine.put(tp.getName().toString(), bound(tp, tvs));
        tvs.push(mine);
        for (Tree m : ct.getMembers()) {
            if (m instanceof ClassTree nested) {
                visitClass(nested, fqn + (iface ? "#iface" : ""), marked, tvs, out);
            } else if (m instanceof MethodTree mt) {
                if (!visible(mt.getModifiers(), iface)) continue;
                boolean mMarked = marked;
                if (has(mt.getModifiers(), "NullMarked")) mMarked = true;
                if (has(mt.getModifiers(), "NullUnmarked")) mMarked = false;
                Map<String, String> mtv = new HashMap<>();
                for (TypeParameterTree tp : mt.getTypeParameters()) mtv.put(tp.getName().toString(), bound(tp, tvs));
                tvs.push(mtv);
                boolean ctor = mt.getReturnType() == null;
                String name = ctor ? "<init>" : mt.getName().toString();
                String ret = ctor ? "-" : state(mt.getReturnType(), mt.getModifiers(), mMarked, tvs);
                StringBuilder ps = new StringBuilder();
                List<String> types = new ArrayList<>(), nestedParams = new ArrayList<>();
                for (VariableTree p : mt.getParameters()) {
                    ps.append(state(p.getType(), p.getModifiers(), mMarked, tvs));
                    types.add(erase(p.getType(), tvs));
                    nestedParams.add(mMarked ? nested(p.getType(), p.getModifiers()) : "");
                }
                String nestedReturn = ctor || !mMarked ? "" : nested(mt.getReturnType(), mt.getModifiers());
                out.add(fqn + "\t" + name + "\t" + String.join(",", types) + "\t" + ret + "\t" + ps + "\t"
                        + (mMarked ? "marked" : "unmarked") + "\t" + (ctor ? "-" : erase(mt.getReturnType(), tvs))
                        + "\t" + nestedReturn + "\t" + String.join(";", nestedParams));
                tvs.pop();
            }
        }
        tvs.pop();
    }

    static boolean nullableAnnotated(Tree t) {
        return t instanceof AnnotatedTypeTree at && at.getAnnotations().stream()
                .anyMatch(a -> lastSeg(a.getAnnotationType().toString()).equals("Nullable"));
    }

    /*
     The positions one level down that may hold null, comma-joined: an array's elements (0), when the annotation in
     declaration position or on the component says so ('@Nullable Object[] toArray()', '@Nullable Object... args');
     a type argument annotated @Nullable, or a wildcard whose bound is ('? super @Nullable V', '? extends @Nullable U').
     */
    static String nested(Tree type, ModifiersTree mods) {
        Tree t = type instanceof AnnotatedTypeTree at ? at.getUnderlyingType() : type;
        if (t instanceof ArrayTypeTree arr) {
            return has(mods, "Nullable") || nullableAnnotated(arr.getType()) ? "0" : "";
        }
        if (!(t instanceof ParameterizedTypeTree ptt)) return "";
        List<String> positions = new ArrayList<>();
        List<? extends Tree> args = ptt.getTypeArguments();
        for (int i = 0; i < args.size(); i++) {
            Tree a = args.get(i);
            if (nullableAnnotated(a) || a instanceof WildcardTree w && w.getBound() != null && nullableAnnotated(w.getBound())) {
                positions.add(String.valueOf(i));
            }
        }
        return String.join(",", positions);
    }

    static String bound(TypeParameterTree tp, Deque<Map<String, String>> tvs) {
        return tp.getBounds().isEmpty() ? "Object" : erase(tp.getBounds().get(0), tvs);
    }

    static String tvBound(String name, Deque<Map<String, String>> tvs) {
        for (Map<String, String> m : tvs) if (m.containsKey(name)) return m.get(name);
        return null;
    }

    static String erase(Tree t, Deque<Map<String, String>> tvs) {
        return switch (t) {
            case AnnotatedTypeTree at -> erase(at.getUnderlyingType(), tvs);
            case ArrayTypeTree at -> erase(at.getType(), tvs) + "[]";
            case ParameterizedTypeTree pt -> erase(pt.getType(), tvs);
            case PrimitiveTypeTree pt -> pt.toString();
            case IdentifierTree id -> {
                String b = tvBound(id.getName().toString(), tvs);
                yield b != null ? b : id.getName().toString();
            }
            case MemberSelectTree ms -> ms.getIdentifier().toString();
            default -> t.toString();
        };
    }

    // varargs: javac gives an ArrayTypeTree; the annotation in modifiers before an array type is on the ELEMENT
    static String state(Tree type, ModifiersTree mods, boolean marked, Deque<Map<String, String>> tvs) {
        if (type instanceof PrimitiveTypeTree) return "-";
        boolean declNullable = has(mods, "Nullable"), declNonNull = has(mods, "NonNull");
        Tree t = type;
        boolean typeNullable = false, typeNonNull = false;
        // Map.@Nullable Entry<K, V>: the annotation sits on the qualified type inside the parameterized one
        if (t instanceof ParameterizedTypeTree ptt && ptt.getType() instanceof AnnotatedTypeTree inner) {
            typeNullable = inner.getAnnotations().stream().anyMatch(a -> lastSeg(a.getAnnotationType().toString()).equals("Nullable"));
            typeNonNull = inner.getAnnotations().stream().anyMatch(a -> lastSeg(a.getAnnotationType().toString()).equals("NonNull"));
        }
        if (t instanceof AnnotatedTypeTree at) {
            typeNullable |= at.getAnnotations().stream().anyMatch(a -> lastSeg(a.getAnnotationType().toString()).equals("Nullable"));
            typeNonNull |= at.getAnnotations().stream().anyMatch(a -> lastSeg(a.getAnnotationType().toString()).equals("NonNull"));
            t = at.getUnderlyingType();
        }
        if (t instanceof ArrayTypeTree) {
            // declaration-position annotations speak about the element; only a type annotation on the array counts
            if (typeNullable) return "Q";
            if (typeNonNull) return "N";
            return marked ? "N" : "U";
        }
        if (declNullable || typeNullable) return "Q";
        if (declNonNull || typeNonNull) return "N";
        Tree base = t instanceof ParameterizedTypeTree pt ? pt.getType() : t;
        if (base instanceof IdentifierTree id && tvBound(id.getName().toString(), tvs) != null) return "P";
        return marked ? "N" : "U";
    }
}
