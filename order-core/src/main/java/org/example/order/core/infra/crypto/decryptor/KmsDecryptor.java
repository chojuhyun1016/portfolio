package org.example.order.core.infra.crypto.decryptor;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.crypto.config.EncryptProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;

import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * AWS KMS를 통해 base64 인코딩된 암호화 키를 복호화하는 컴포넌트
 * Spring 컨테이너에 빈으로 등록되며 재사용 가능
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KmsDecryptor {

    private final EncryptProperties encryptProperties;

    /**
     * Base64 인코딩된 KMS 암호화 키를 복호화하여 byte[]로 반환
     *
     * @param base64Key KMS로 암호화된 키 (base64 문자열)
     * @return 복호화된 키 byte 배열
     */
    public byte[] decryptBase64EncodedKey(String base64Key) {
        try {
            byte[] encryptedKey = Base64.getDecoder().decode(base64Key);

            try (KmsClient kmsClient = KmsClient.builder()
                    .region(Region.of(encryptProperties.getKmsRegion()))
                    .build()) {

                DecryptRequest decryptRequest = DecryptRequest.builder()
                        .ciphertextBlob(SdkBytes.fromByteBuffer(ByteBuffer.wrap(encryptedKey)))
                        .build();

                SdkBytes decrypted = kmsClient.decrypt(decryptRequest).plaintext();

                return decrypted.asByteArray();
            }

        } catch (Exception e) {
            log.error("[KMS] 복호화 실패 - base64Key: {}", base64Key, e);
            throw new RuntimeException("KMS 복호화 실패", e);
        }
    }
}
