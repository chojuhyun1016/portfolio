package org.example.order.core.infra.security.gateway.matcher;

import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * 화이트리스트 경로 매처:
 * - 설정 기반 + 프로그램적 화이트리스트 관리 가능
 */
public class WhiteListMatcher {

    protected final List<String> whiteList;
    protected final AntPathMatcher pathMatcher = new AntPathMatcher();

    public WhiteListMatcher(List<String> initialWhiteList) {
        this.whiteList = new ArrayList<>(initialWhiteList);
    }

    /**
     * 화이트리스트 추가 메서드 (동적으로 경로 추가 가능)
     */
    public void addWhiteList(String pattern) {
        whiteList.add(pattern);
    }

    /**
     * 경로가 화이트리스트에 포함되는지 확인
     */
    public boolean isWhiteListed(String path) {
        return whiteList.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}
