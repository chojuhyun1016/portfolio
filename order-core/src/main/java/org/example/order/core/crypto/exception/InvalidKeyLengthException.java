package org.example.order.core.crypto.exception;

public class InvalidKeyLengthException extends EncryptException {
    public InvalidKeyLengthException(int expected, int actual) {
        super("Expected key length: " + expected + " but got: " + actual);
    }
}
