// `transitive`: the annotations moved to maddi-annotation at 0.9.1, but anything that reads
// io.codelaser.maddi.support types also sees the annotations on them, so consumers must not need a
// second `requires`. This is the module's only dependency, and it is on a sibling published to
// Central alongside it -- see PUBLISHING.md before adding a second one.
module io.codelaser.maddi.support {
    requires transitive io.codelaser.maddi.annotation;

    exports io.codelaser.maddi.annotatedapi;
    exports io.codelaser.maddi.support;
}