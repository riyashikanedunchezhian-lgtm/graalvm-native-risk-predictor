package com.example;

public class App {
    public static void main(String[] args) {
        System.out.println("Hello from a native-image-friendly app.");
    }

    public int add(int a, int b) {
        return a + b;
    }
}
