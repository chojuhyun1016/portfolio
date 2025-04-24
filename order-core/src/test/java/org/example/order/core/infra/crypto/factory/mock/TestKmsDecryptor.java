// ✅ File: org.example.order.core.infra.crypto.factory.mock.TestKmsDecryptor.java
package org.example.order.core.infra.crypto.factory.mock;

import org.example.order.core.infra.common.kms.config.KmsProperties;
import org.example.order.core.infra.common.kms.decryptor.KmsDecryptor;

import java.util.Base64;

public class TestKmsDecryptor extends KmsDecryptor {

    public TestKmsDecryptor(KmsProperties kmsProperties) {
        super(kmsProperties);
    }

    @Override
    public byte[] decryptBase64EncodedKey(String base64Key) {
        // 실제 복호화 대신 base64 디코딩만 수행
        return Base64.getDecoder().decode(base64Key);
    }
}
