package org.example.order.core.infra.common.secrets.manager;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpecEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SecretsKeyResolver
 * ------------------------------------------------------------------------
 * [요약]
 * - alias별 다중 키(객체/배열) 스냅샷 저장 + 선택 포인터 관리.
 * - 선택 규칙: kid 우선 -> version -> (옵션) 최신.
 * <p>
 * [핵심 포인트]
 * - 운영 기본: 자동 최신 금지(allowLatest=false). 설정에 따른 “핀 고정”만 반영.
 * - getKey(alias, version, kid)로 과거 버전도 직접 조회 가능(롱테일 복호화 시 유용).
 * - 종료 시 보안 위해 wipeAll(): 메모리 키 zero-fill & 포인터/스토어 정리.
 */
@Slf4j
public class SecretsKeyResolver {

    // alias → 전체 키 스냅샷(여러 버전/여러 kid 가능)
    private final Map<String, List<CryptoKeySpecEntry>> store = new ConcurrentHashMap<>();

    // alias → 현재 선택된 키(운영 정책에 따라 kid 또는 version으로 핀)
    private final Map<String, AtomicReference<CryptoKeySpecEntry>> pointer = new ConcurrentHashMap<>();

    /**
     * SecretsLoader가 파싱한 결과를 alias 단위로 스냅샷 반영.
     * (객체/배열 모두 CryptoKeySpecEntry 목록으로 정규화되어 들어옴)
     */
    public void setSnapshot(String alias, List<CryptoKeySpecEntry> entries) {
        if (entries == null) {
            entries = List.of();
        }

        store.put(alias, entries);
        pointer.putIfAbsent(alias, new AtomicReference<>());

        log.info("[Secrets] snapshot set: alias={}, size={}", alias, entries.size());
    }

    /**
     * 운영 정책에 따라 현재 사용할 키 선택(포인터 고정)
     * - kid -> version -> (allowLatest=true면) 최신 순으로 선택
     */
    public boolean applySelection(String alias, Integer version, String kid, boolean allowLatest) {
        List<CryptoKeySpecEntry> list = store.getOrDefault(alias, List.of());

        if (list.isEmpty()) {
            return false;
        }

        CryptoKeySpecEntry selected = null;

        // 1) kid 우선
        if (kid != null && !kid.isBlank()) {
            selected = list.stream()
                    .filter(e -> kid.equals(e.getKid()))
                    .findFirst()
                    .orElse(null);
        }

        // 2) version
        if (selected == null && version != null) {
            selected = list.stream()
                    .filter(e -> Objects.equals(version, e.getVersion()))
                    .findFirst()
                    .orElse(null);
        }

        // 3) 최신 허용(운영 기본: false)
        if (selected == null && allowLatest) {
            selected = list.stream()
                    .max(Comparator.comparing(e -> Optional.ofNullable(e.getVersion()).orElse(0)))
                    .orElse(list.get(0));
        }

        if (selected == null) {
            return false;
        }

        pointer.computeIfAbsent(alias, k -> new AtomicReference<>()).set(selected);

        log.info("[Secrets] pointer -> alias={}, kid={}, version={}", alias, selected.getKid(), selected.getVersion());

        return true;
    }

    /**
     * 현재 선택된 키 바이트(암/복호화에 사용)
     * - selection이 한 번도 적용되지 않았다면 예외
     */
    public byte[] getCurrentKey(String alias) {
        AtomicReference<CryptoKeySpecEntry> ref = pointer.get(alias);

        if (ref == null || ref.get() == null) {
            throw new IllegalStateException("No selected key for alias=" + alias);
        }

        return ref.get().getKeyBytes();
    }

    /**
     * 과거 버전/특정 kid 키 바이트 조회(롱테일 복호화 등을 위해)
     * - kid가 우선. 둘 다 null이면 null 반환
     */
    public byte[] getKey(String alias, Integer version, String kid) {
        List<CryptoKeySpecEntry> list = store.getOrDefault(alias, List.of());

        if (list.isEmpty()) {
            return null;
        }

        if (kid != null && !kid.isBlank()) {
            return list.stream()
                    .filter(e -> kid.equals(e.getKid()))
                    .map(CryptoKeySpecEntry::getKeyBytes)
                    .findFirst()
                    .orElse(null);
        }

        if (version != null) {
            return list.stream()
                    .filter(e -> Objects.equals(version, e.getVersion()))
                    .map(CryptoKeySpecEntry::getKeyBytes)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    /**
     * 종료/회수 시 보안 강화를 위한 메모리 키 wipe & clear
     * - store의 모든 entry.keyBytes zero-fill
     * - pointer의 현재 선택 키도 zero-fill
     * - 맵 clear
     */
    public void wipeAll() {
        try {
            // 1) store 쪽 키 바이트 덮어쓰기
            store.forEach((alias, list) -> {
                if (list != null) {
                    for (CryptoKeySpecEntry e : list) {
                        byte[] kb = e.getKeyBytes();

                        if (kb != null) {
                            Arrays.fill(kb, (byte) 0);
                        }
                    }
                }
            });

            // 2) pointer 쪽 현재 선택 키도 덮어쓰기
            pointer.forEach((alias, ref) -> {
                CryptoKeySpecEntry cur = (ref != null) ? ref.get() : null;

                if (cur != null && cur.getKeyBytes() != null) {
                    Arrays.fill(cur.getKeyBytes(), (byte) 0);
                    ref.set(null);
                }
            });

            // 3) 맵 비우기
            store.clear();
            pointer.clear();

            log.info("[Secrets] wipeAll completed (keys zero-filled & maps cleared).");
        } catch (Throwable t) {
            log.warn("[Secrets] wipeAll() failed/partial: {}", t.toString());
        }
    }
}
