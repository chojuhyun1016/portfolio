package org.example.order.core.infra.common.secrets.listener;

/**
 * 시크릿 키 변경 리스너 인터페이스.
 * SecretsLoader가 키 리프레시 후 이 리스너를 호출.
 */
public interface SecretKeyRefreshListener {
    /**
     * 키가 새로 로드될 때 호출됨.
     */
    void onSecretKeyRefreshed();
}
