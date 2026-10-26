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

package io.codelaser.maddi.cst.print.rust;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.print.translate.CstQueries;

import java.util.List;
import java.util.Set;

/**
 * Rust's names for Java's: snake case for functions, fields and locals, the type's own name for a type, a module per
 * package. Rust has no overloading, so the second and later same-named methods of a type get a numeric suffix, in
 * declaration order, as constructors do ({@code new}, {@code new_2}, …): the declaration and every call agree.
 */
public final class RustNames {

    private static final Set<String> KEYWORDS = Set.of("as", "async", "await", "break", "const", "continue", "crate",
            "dyn", "else", "enum", "extern", "false", "fn", "for", "if", "impl", "in", "let", "loop", "match", "mod",
            "move", "mut", "pub", "ref", "return", "static", "struct", "super", "trait", "true", "type", "unsafe",
            "use", "where", "while", "abstract", "become", "box", "do", "final", "macro", "override", "priv", "try",
            "typeof", "unsized", "virtual", "yield", "gen");

    /** Names that cannot be raw identifiers either. */
    private static final Set<String> NOT_RAW = Set.of("self", "Self", "super", "crate");

    private RustNames() {
    }

    /** {@code getValueAt} becomes {@code get_value_at}, {@code URLParser} {@code url_parser}. */
    public static String snake(String javaName) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < javaName.length(); i++) {
            char c = javaName.charAt(i);
            if (Character.isUpperCase(c)) {
                boolean previousLower = i > 0 && (Character.isLowerCase(javaName.charAt(i - 1))
                                                  || Character.isDigit(javaName.charAt(i - 1)));
                boolean nextLower = i + 1 < javaName.length() && Character.isLowerCase(javaName.charAt(i + 1));
                boolean previousUpper = i > 0 && Character.isUpperCase(javaName.charAt(i - 1));
                if (i > 0 && javaName.charAt(i - 1) != '_' && (previousLower || previousUpper && nextLower)) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return identifier(sb.toString());
    }

    /** A constant's name: Java's {@code MAX_SIZE} is already Rust's. */
    public static String constant(String javaName) {
        return javaName.equals(javaName.toUpperCase()) ? javaName : snake(javaName).toUpperCase();
    }

    /** A keyword as a raw identifier: {@code type} becomes {@code r#type}. */
    public static String identifier(String name) {
        if (NOT_RAW.contains(name)) return name + "_";
        return KEYWORDS.contains(name) ? "r#" + name : name;
    }

    /** The method's name, with the suffix of its rank among the same-named methods of its type. */
    public static String method(MethodInfo method) {
        if (method.isConstructor()) return "new" + suffix(method, method.typeInfo().constructors());
        List<MethodInfo> sameName = method.typeInfo().methods().stream()
                .filter(m -> m.name().equals(method.name())).toList();
        return snake(method.name()) + suffix(method, sameName);
    }

    private static String suffix(MethodInfo method, List<MethodInfo> overloads) {
        int index = overloads.indexOf(method);
        return index <= 0 ? "" : "_" + (index + 1);
    }

    /** An enum constant as a variant: {@code DARK_GREEN} becomes {@code DarkGreen}. */
    public static String variant(String constant) {
        StringBuilder sb = new StringBuilder();
        for (String part : constant.toLowerCase().split("_")) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    /** The module of a package: {@code com.example.util} becomes {@code com_example_util}. */
    public static String module(String packageName) {
        return packageName.isEmpty() ? "root" : identifier(packageName.replace('.', '_'));
    }

    /** The path from the crate root: {@code crate::com_example_util::Parser}. */
    public static String path(TypeInfo typeInfo) {
        return "crate::" + module(typeInfo.packageName()) + "::" + type(typeInfo);
    }

    /**
     * A nested type's name joins the enclosing ones: {@code Outer.Inner} becomes {@code OuterInner}. A name that is
     * one of Rust's own types, which the translation names unqualified, gets a {@code J} in front: a class {@code Box}
     * printed as {@code Box} made every {@code Box<dyn Fn…>} of its module that struct (E0618, E0277).
     */
    public static String type(TypeInfo typeInfo) {
        TypeInfo enclosing = CstQueries.enclosing(typeInfo);
        String name = enclosing == null ? typeInfo.simpleName() : type(enclosing) + typeInfo.simpleName();
        return RUST_TYPES.contains(name) ? "J" + name : name;
    }

    /** The prelude's types and traits, and the standard types the translation uses without a path. */
    private static final Set<String> RUST_TYPES = Set.of("Box", "Vec", "String", "Option", "Result", "Some", "None",
            "Ok", "Err", "Self", "Rc", "RefCell", "Cell", "LazyLock", "PhantomData", "Any", "HashMap", "HashSet",
            "BTreeMap", "BTreeSet", "VecDeque", "SystemTime", "Duration", "Fn", "FnMut", "FnOnce", "Clone", "Copy",
            "Send", "Sync", "Sized", "Drop", "Default", "Iterator", "IntoIterator", "ToString", "ToOwned", "From",
            "Into", "AsRef", "AsMut", "Eq", "PartialEq", "Ord", "PartialOrd", "Hash", "Debug", "Display", "Extend");
}
