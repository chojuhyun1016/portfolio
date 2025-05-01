package org.example.order.core.infra.common.secrets.manager;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 다중 알고리즘 키 핫스왑 + 롤백 관리 컴포넌트
 */
@Slf4j
@Component
public class SecretsKeyResolver {

    private final Map<String, AtomicReference<byte[]>> currentKeyMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicReference<byte[]>> backupKeyMap = new ConcurrentHashMap<>();

    /**
     * 새 키 업데이트 (핫스왑)
     */
    public void updateKey(String keyName, CryptoKeySpec keySpec) {
        currentKeyMap.putIfAbsent(keyName, new AtomicReference<>());
        backupKeyMap.putIfAbsent(keyName, new AtomicReference<>());

        AtomicReference<byte[]> currentRef = currentKeyMap.get(keyName);
        AtomicReference<byte[]> backupRef = backupKeyMap.get(keyName);

        byte[] newKey = keySpec.decodeKey();
        byte[] oldKey = currentRef.get();

        if (oldKey != null && !java.util.Arrays.equals(oldKey, newKey)) {
            backupRef.set(oldKey);
            log.info("[SecretsKeyResolver] [{}] Previous key backed up.", keyName);
        }

        currentRef.set(newKey);
        log.info("[SecretsKeyResolver] [{}] New key updated. Spec: {}", keyName, keySpec);
    }

    public byte[] getCurrentKey(String keyName) {
        AtomicReference<byte[]> keyRef = currentKeyMap.get(keyName);

        if (keyRef == null || keyRef.get() == null) {
            throw new IllegalStateException("Secret key for [" + keyName + "] is not loaded yet.");
        }

        return keyRef.get();
    }

    public byte[] getBackupKey(String keyName) {
        AtomicReference<byte[]> backupRef = backupKeyMap.get(keyName);
        return backupRef != null ? backupRef.get() : null;
    }
}
