
package a;
public class X {
    enum State { START, BUSY, E ND }

    int method(State state) {
        return switch(state) {
            case START -> 3;
            case END -> 4;
            default -> 0;
        };
    }
}