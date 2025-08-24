// 파일: order-core/src/integrationTest/java/org/example/order/core/infra/common/secrets/testutil/TestKeys.java
package org.example.order.core.infra.common.secrets.testutil;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * 통합테스트 전용 TestKeys 브릿지.
 *
 * ⚠️ 본 클래스는 unit test의 TestKeys에 의존하지 않고,
 *     integrationTest 소스셋에서 “각자 선언”하여 충돌 없이 사용하기 위한 최소 구현이다.
 *
 * - 기존 테스트 코드의 정적 임포트(import static ... TestKeys.std;)를 그대로 유지한다.
 * - 문자열 조합용 std(String...) + 키 생성용 std(int) 오버로드를 함께 제공한다.
 *   → Secrets*IT 에서 std(16), std(32) 같은 호출을 그대로 지원.
 *
 * 구현 상세:
 * - std(int) 은 length 바이트의 난수를 생성한 뒤 Base64 문자열로 반환한다.
 *   (테스트 대상 CryptoKeySpec#setValue(...) 가 String 시그니처이기 때문)
 * - 길이 규칙 검증이 필요하면, 해당 IT 안에서 Base64 디코딩 후 바이트 길이를 체크하면 된다.
 */
public final class TestKeys {

    private static final SecureRandom RNG = new SecureRandom();

    private TestKeys() {}

    /**
     * 표준 키 조합 도우미 (가변 인자).
     * - 인자가 없으면 빈 문자열을 반환한다.
     * - 인자가 하나면 그대로 반환한다.
     * - 인자가 여러 개면 ":" 로 연결한다.
     */
    public static String std(String... parts) {
        if (parts == null || parts.length == 0) return "";
        if (parts.length == 1) return Objects.toString(parts[0], "");
        return String.join(":", parts);
    }

    /**
     * length 바이트 난수를 생성해 Base64 문자열로 반환.
     * - Secrets*IT 에서 CryptoKeySpec#setValue(std(16|32)) 호출을 지원하기 위한 오버로드.
     */
    public static String std(int length) {
        if (length < 0) throw new IllegalArgumentException("length must be >= 0");
        byte[] key = new byte[length];
        RNG.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
