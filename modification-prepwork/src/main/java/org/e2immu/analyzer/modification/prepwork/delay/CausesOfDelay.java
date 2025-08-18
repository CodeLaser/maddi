package org.e2immu.analyzer.modification.prepwork.delay;

public class CausesOfDelay {
    public static final CausesOfDelay NO_DELAY = new CausesOfDelay(false);
    public static final CausesOfDelay DELAY = new CausesOfDelay(true);

    private final boolean delayed;

    private CausesOfDelay(boolean delayed) {
        this.delayed = delayed;
    }

    public boolean isDelayed() {
        return delayed;
    }
}
