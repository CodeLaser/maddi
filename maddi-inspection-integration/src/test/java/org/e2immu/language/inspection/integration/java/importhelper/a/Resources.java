package org.e2immu.language.inspection.integration.java.importhelper.a;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PACKAGE})
public @interface Resources {
    Resource[] value();
}
