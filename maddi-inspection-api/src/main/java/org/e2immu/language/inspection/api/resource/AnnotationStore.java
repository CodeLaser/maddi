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
