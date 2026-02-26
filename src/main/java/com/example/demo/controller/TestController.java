package com.example.demo.controller;

public class TestController {

    private boolean isValid = true;

    private boolean usedValue;

    public String testMethod1() {
        return isValid ? "This is the correct value." : "This is the wrong value.";
    }

    public String testMethod() {
        return isValid ? "This is the correct value." : "This is the wrong value.";
    }
}