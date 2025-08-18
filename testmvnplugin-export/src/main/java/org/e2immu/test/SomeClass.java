package org.e2immu.test;

public record SomeClass(int i) {
    void print() {
        System.out.println(i);
    }
}