package org.example.order.core.crypto.exception;

public class SignException extends RuntimeException {
    public SignException(String message) {
        super(message);
    }

    public SignException(String message, Throwable cause) {
        super(message, cause);
    }
}
