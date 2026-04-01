package com.example.demo.exception;

public class CodeFileException extends RuntimeException {

    public CodeFileException(String message) {
        super(message);
    }

    public CodeFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
