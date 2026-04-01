package com.example.demo.controller;

/**
 * Controlador de prueba.
 */
public class TestController {

    private final boolean isValid;

    public TestController() {
        this(true);
    }

    TestController(boolean isValid) {
        this.isValid = isValid;
    }

    /**
     * Método de prueba que devuelve un mensaje según el valor de isValid.
     * 
     * @return Mensaje de prueba.
     */
    public String testMethod() {
        return isValid ? "Este es el valor correcto" : "Este es el valor incorrecto";
    }
}