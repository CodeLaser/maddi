module org.e2immu.language.inspection.api {
    requires org.e2immu.util.external.support;
    requires org.e2immu.language.cst.api;
    requires org.e2immu.util.internal.util;

    requires org.slf4j;

    exports org.e2immu.language.inspection.api.integration;
    exports org.e2immu.language.inspection.api.parser;
    exports org.e2immu.language.inspection.api.resource;
    exports org.e2immu.language.inspection.api.util;

}