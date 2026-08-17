package io.codelaser.maddi.test;

public record SomeClass(int i) {
    void print() {
        System.out.println(i);
    }
}