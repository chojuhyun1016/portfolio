package org.example.order.core.infra.common.kms.decryptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.common.kms.config.KmsProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;

import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * 공통 KMS 유틸리티 컴포넌트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KmsDecryptor {

    private final KmsProperties kmsProperties;

    /**
     * base64 인코딩된 암호화된 키를 KMS를 통해 복호화
     * @param base64Key base64 문자열로 인코딩된 암호화된 키
     * @return 복호화된 byte 배열
     */
    public byte[] decryptBase64EncodedKey(String base64Key) {
        try {
            byte[] encryptedKey = Base64.getDecoder().decode(base64Key);

            try (KmsClient kmsClient = KmsClient.builder()
                    .region(Region.of(kmsProperties.getRegion()))
                    .build()) {

                DecryptRequest request = DecryptRequest.builder()
                        .ciphertextBlob(SdkBytes.fromByteBuffer(ByteBuffer.wrap(encryptedKey)))
                        .build();

                return kmsClient.decrypt(request).plaintext().asByteArray();
            }

        } catch (Exception e) {
            log.error("[KMS] 복호화 실패 - base64Key: {}", base64Key, e);
            throw new RuntimeException("KMS 복호화 실패", e);
        }
    }
}
