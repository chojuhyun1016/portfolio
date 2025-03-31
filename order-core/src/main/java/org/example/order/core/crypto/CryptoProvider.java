package org.example.order.core.crypto;

import org.example.order.core.crypto.code.CryptoAlgorithmType;

public interface CryptoProvider {
    CryptoAlgorithmType getType();
    boolean isReady();
}
