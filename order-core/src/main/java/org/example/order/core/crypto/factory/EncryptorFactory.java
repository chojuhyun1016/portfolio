package org.example.order.core.crypto.factory;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.crypto.Encryptor;
import org.example.order.core.crypto.code.EncryptorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EncryptorFactory {

    private final Map<EncryptorType, Encryptor> encryptorMap;

    public EncryptorFactory(List<Encryptor> encryptors) {
        this.encryptorMap = Map.copyOf(
                encryptors.stream()
                        .collect(Collectors.toMap(
                                Encryptor::getType,
                                e -> e
                        ))
        );

        log.info("EncryptorFactory initialized with: {}", encryptorMap.keySet());
    }

    public Encryptor getEncryptor(EncryptorType type) {
        return Objects.requireNonNull(encryptorMap.get(type),
                () -> "Unsupported EncryptorType: " + type);
    }
}
