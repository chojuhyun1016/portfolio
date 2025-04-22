package org.example.order.core.infra.crypto.contract;

import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;

public interface CryptoProvider {
    CryptoAlgorithmType getType();
    boolean isReady();
}
