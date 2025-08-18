package org.e2immu.language.cst.api.element;

/**
 * Represents any object that can be serialized using the cst-io system that can be used to determine
 * whether a source or set of sources has changed.
 * Typical implementation is an MD-5 hash.
 * <p>
 * Only relevant methods are <code>Object.equals()</code> and <code>Object.toString()</code>.
 */
public interface FingerPrint {

    boolean isNoFingerPrint();

}
