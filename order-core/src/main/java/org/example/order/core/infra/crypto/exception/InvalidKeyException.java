package org.example.order.core.infra.crypto.exception;

public class InvalidKeyException extends EncryptException {
    public InvalidKeyException(String key) {
        super("Invalid key : " + key);
    }

    public InvalidKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
