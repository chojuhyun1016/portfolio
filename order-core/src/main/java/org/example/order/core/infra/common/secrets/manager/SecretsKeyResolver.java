package org.example.order.core.infra.common.secrets.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Secret Key 핫스왑 + 롤백 관리 컴포넌트
 */
@Slf4j
@Component
public class SecretsKeyResolver {

    private final AtomicReference<byte[]> currentKeyRef = new AtomicReference<>();
    private final AtomicReference<byte[]> backupKeyRef = new AtomicReference<>();

    /**
     * 새로운 키로 업데이트 (핫스왑)
     */
    public void updateKey(byte[] newKey) {
        byte[] oldKey = currentKeyRef.get();

        if (oldKey != null && !Arrays.equals(oldKey, newKey)) {
            backupKeyRef.set(oldKey);
            log.info("[SecretsKeyResolver] Previous key backed up for rollback.");
        }

        currentKeyRef.set(newKey);
    }

    /**
     * 현재 활성화된 키 조회
     */
    public byte[] getCurrentKey() {
        byte[] key = currentKeyRef.get();

        if (key == null) {
            throw new IllegalStateException("Secret key has not been loaded yet.");
        }

        return key;
    }

    /**
     * 복호화 실패 시 백업 키 조회
     */
    public byte[] getBackupKey() {
        return backupKeyRef.get();
    }
}
