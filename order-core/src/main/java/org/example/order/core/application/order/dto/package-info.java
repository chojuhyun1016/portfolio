/**
 * Application 내부 DTO 모음.
 * <p>
 * 하위 패키지 규칙:
 * <ul>
 *   <li>{@code command} : 유스케이스 입력(행위 지시) DTO. Controller Request → RequestMapper → Command.</li>
 *   <li>{@code query}   : 조회 입력/조건 DTO. Controller Request → RequestMapper → Query.</li>
 *   <li>{@code view}    : 유스케이스 결과(내부 표현) DTO. Service → View → ResponseMapper → API Response.</li>
 *   <li>{@code sync}    : 동기화/파이프라인 내부 DTO. 메시징·CDC 보조(내부 전송/재처리/로그/추적).</li>
 * </ul>
 * <p>
 * 주의:
 * <ul>
 *   <li>이 패키지의 DTO는 외부 계약(HTTP/메시지)로 직접 노출하지 않는다.</li>
 *   <li>Entity/Aggregate를 import하거나 직접 참조하지 않는다.</li>
 *   <li>시간 타입과 null 정책은 내부 기준으로 일관되게 유지하고, 외부 형식(Epoch millis 등) 변환은 매퍼에서 처리한다.</li>
 * </ul>
 */
package org.example.order.core.application.order.dto;
