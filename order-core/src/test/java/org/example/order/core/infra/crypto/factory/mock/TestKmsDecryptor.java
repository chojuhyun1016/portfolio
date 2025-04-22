package org.example.order.core.infra.crypto.factory.mock;

import org.example.order.core.infra.crypto.config.EncryptProperties;
import org.example.order.core.infra.crypto.decryptor.KmsDecryptor;

import java.util.Base64;

public class TestKmsDecryptor extends KmsDecryptor {

    public TestKmsDecryptor(EncryptProperties encryptProperties) {
        super(encryptProperties);
    }

    @Override
    public byte[] decryptBase64EncodedKey(String base64Key) {
        // 실제 복호화 대신 base64 디코딩만 수행
        return Base64.getDecoder().decode(base64Key);
    }
}
