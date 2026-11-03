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

package io.codelaser.maddi.run.rewire;

import java.util.List;

/**
 * A diagnostic for the rewire driver's leak check: the shortest chain of references (by fields, map entries,
 * collection elements, array slots) from a root to an object that should have been collected. Reflection only, so it
 * cannot see through a field it is not allowed to open, nor find a static root it is not given.
 */
final class LeakPath {
    private LeakPath() {
    }

    static String path(Object root, Object target) {
        java.util.IdentityHashMap<Object, Object[]> parent = new java.util.IdentityHashMap<>();
        java.util.ArrayDeque<Object> queue = new java.util.ArrayDeque<>();
        parent.put(root, new Object[]{null, "root"});
        queue.add(root);
        while (!queue.isEmpty()) {
            Object o = queue.poll();
            if (o == target) {
                StringBuilder sb = new StringBuilder();
                Object c = o;
                while (c != null) {
                    Object[] pe = parent.get(c);
                    sb.insert(0, "\n  " + pe[1] + " : " + c.getClass().getName()
                                 + (c instanceof io.codelaser.maddi.cst.api.info.Info info ? " (" + info + ")" : ""));
                    c = pe[0];
                }
                return sb.toString();
            }
            for (Object[] edge : edges(o)) {
                Object child = edge[0];
                if (child == null || parent.containsKey(child)) continue;
                if (child instanceof String || child instanceof Number || child instanceof Class
                    || child instanceof Thread || child instanceof ClassLoader || child instanceof Boolean
                    || child instanceof Enum<?>) continue;
                parent.put(child, new Object[]{o, edge[1]});
                queue.add(child);
            }
        }
        return "not found, " + parent.size() + " objects visited";
    }

    static List<Object[]> edges(Object o) {
        List<Object[]> out = new java.util.ArrayList<>();
        Class<?> c = o.getClass();
        if (c.isArray()) {
            if (!c.getComponentType().isPrimitive()) {
                Object[] a = (Object[]) o;
                for (int i = 0; i < a.length; i++) out.add(new Object[]{a[i], "[" + i + "]"});
            }
            return out;
        }
        if (o instanceof java.util.Map<?, ?> m && !c.getName().startsWith("io.codelaser")) {
            try {
                for (var e : m.entrySet()) {
                    out.add(new Object[]{e.getKey(), "key"});
                    out.add(new Object[]{e.getValue(), "value"});
                }
            } catch (RuntimeException re) { /* ignore */ }
            return out;
        }
        if (o instanceof java.util.Collection<?> col && !c.getName().startsWith("io.codelaser")) {
            try {
                int i = 0;
                for (Object e : col) out.add(new Object[]{e, "elem" + (i++)});
            } catch (RuntimeException re) { /* ignore */ }
            return out;
        }
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) continue;
                try {
                    f.setAccessible(true);
                    out.add(new Object[]{f.get(o), k.getSimpleName() + "." + f.getName()});
                } catch (RuntimeException | IllegalAccessException e) { /* inaccessible */ }
            }
        }
        return out;
    }
}
