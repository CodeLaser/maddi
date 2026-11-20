package io.codelaser.maddi.cst.impl.analysis;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.type.NullableState;

/** The encoded shape of {@link ValueImpl.NullabilityImpl}: what a results file holds. */
public class TestNullabilityValue {

    @Test
    public void shapeRoundTrips() {
        for (String shape : List.of("U", "N", "Q", "U(N,Q)", "N(Q(N,U),N)", "Q(U)")) {
            assertEquals(shape, ValueImpl.NullabilityImpl.fromShape(shape).shape());
        }
        ValueImpl.NullabilityImpl map = ValueImpl.NullabilityImpl.fromShape("U(N,Q)");
        assertEquals(NullableState.UNSPECIFIED, map.state());
        assertEquals(NullableState.NULLABLE, map.arguments().get(1).state());
        assertTrue(ValueImpl.NullabilityImpl.fromShape("U").isDefault());
        assertFalse(map.isDefault());
        assertThrows(IllegalArgumentException.class, () -> ValueImpl.NullabilityImpl.fromShape("X"));
        assertThrows(IllegalArgumentException.class, () -> ValueImpl.NullabilityImpl.fromShape("N)"));
    }
}
