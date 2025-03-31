package org.example.order.core.crypto.exception;

public class InvalidKeyException extends EncryptException {
    public InvalidKeyException(String key) {
        super("Invalid key : " + key);
    }

    public InvalidKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
