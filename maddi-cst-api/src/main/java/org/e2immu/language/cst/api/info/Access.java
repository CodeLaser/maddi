package org.e2immu.language.cst.api.info;

public interface Access {

    /*
    returns values starting from 0, with 0 being the most restrictive.
    In the case of Java, this is private==0, package==1, protected==2, public==3
     */
    int level();

    Access combine(Access other);

    Access max(Access other);

    default boolean ge(Access other) {
        return level() >= other.level();
    }

    default boolean le(Access other) {
        return level() <= other.level();
    }

    boolean isPublic();

    boolean isPrivate();

    boolean isProtected();

    boolean isPackage();
}
