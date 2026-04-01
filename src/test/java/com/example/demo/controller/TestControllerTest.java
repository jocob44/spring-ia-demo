package com.example.demo.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestControllerTest {

    @Test
    void testMethod_ReturnsCorrectMessage_WhenIsValidTrue() {
        // Arrange
        TestController testController = new TestController();

        // Act
        String result = testController.testMethod();

        // Assert
        assertEquals("Este es el valor correcto", result);
    }

    @Test
    void testMethod_ReturnsIncorrectMessage_WhenIsValidFalse() {
        // Arrange
        TestController testController = new TestController(false);

        // Act
        String result = testController.testMethod();

        // Assert
        assertEquals("Este es el valor incorrecto", result);
    }
}