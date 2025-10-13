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

package org.e2immu.language.inspection.api.resource;

import java.util.List;
import java.util.Map;

public interface AnnotationStore {

    TypeItem typeItemsByFQName(String fqName);

    interface  Annotation {

        String name();

        List<KeyValuePair> values();
    }
    interface KeyValuePair {
        String name();
        String value();
    }

    interface Item {
        List<Annotation> annotations();
    }
    interface  TypeItem extends Item{

        Map<String, MethodItem> methodItemMap();
        Map<String, FieldItem> fieldItemMap();
    }

    interface FieldItem extends Item {

    }
    interface ParameterItem extends Item {

        int index();
    }
    interface MethodItem extends Item {

        ParameterItem[] parameterItems();
    }
}
