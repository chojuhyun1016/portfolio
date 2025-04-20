package org.example.order.core.infra.crypto;

import org.example.order.core.infra.crypto.code.CryptoAlgorithmType;

public interface CryptoProvider {
    CryptoAlgorithmType getType();
    boolean isReady();
}
