package org.example.order.core.crypto.factory;

import org.example.order.core.crypto.Encryptor;
import org.example.order.core.crypto.Hasher;
import org.example.order.core.crypto.Signer;
import org.example.order.core.crypto.code.CryptoAlgorithmType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class EncryptorFactory {

    private final Map<CryptoAlgorithmType, Encryptor> encryptors;
    private final Map<CryptoAlgorithmType, Hasher> hashers;
    private final Map<CryptoAlgorithmType, Signer> signers;

    public EncryptorFactory(List<Encryptor> encryptors, List<Hasher> hashes, List<Signer> signers) {
        this.encryptors = encryptors.stream().collect(Collectors.toMap(Encryptor::getType, e -> e));
        this.hashers = hashes.stream().collect(Collectors.toMap(Hasher::getType, h -> h));
        this.signers = signers.stream().collect(Collectors.toMap(Signer::getType, s -> s));
    }

    public Encryptor getEncryptor(CryptoAlgorithmType type) {
        return Objects.requireNonNull(encryptors.get(type), () -> "Unsupported encryptor: " + type);
    }

    public Hasher getHasher(CryptoAlgorithmType type) {
        return Objects.requireNonNull(hashers.get(type), () -> "Unsupported hasher: " + type);
    }

    public Signer getSigner(CryptoAlgorithmType type) {
        return Objects.requireNonNull(signers.get(type), () -> "Unsupported signer: " + type);
    }
}
