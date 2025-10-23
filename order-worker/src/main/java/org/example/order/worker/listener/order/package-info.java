/**
 * # Kafka Listener 테스트용 JSON 샘플 (Order CRUD)
 * <p>
 * 이 패키지는 **CRUD 리스너(OrderCrudMessageListener)** 테스트에 사용할 수 있는
 * Kafka JSON 전문을 정리해 제공합니다. 아래 샘플을 그대로 복사해
 * `kafka-console-producer.sh` 등에 입력하면 됩니다.
 *
 * <p><b>대상 토픽</b>: <code>local-order-crud</code> (로컬 프로파일 기준)</p>
 *
 * <p><b>컨슈머 설정 참고</b>:
 * <ul>
 *   <li><code>spring.json.value.default.type=org.example.order.contract.order.messaging.event.OrderCrudMessage</code></li>
 *   <li>리스너: <code>OrderCrudMessageListenerImpl</code></li>
 *   <li>그룹: <code>group-order-crud</code></li>
 *   <li>배치 컨테이너 팩토리 사용</li>
 * </ul>
 * </p>
 * <p>
 * <hr/>
 * <p>
 * ## 전송 규칙
 * - <b>operation</b>: <code>CREATE | UPDATE | DELETE</code>
 * - <b>orderType</b>: 고정값 <code>"ORDER_CRUD"</code> (계약 상 유형 표기)
 * - <b>payload</b>: 주문 데이터(계약 DTO) — <code>orderId</code>는 필수
 * - 날짜시각: ISO-8601 (예: <code>2025-09-21T06:00:00</code>)
 * - 불리언: <code>true|false</code>
 * - 누락 가능 필드는 <code>null</code> 허용 (DELETE 등)
 * <p>
 * <hr/>
 * <p>
 * ## 샘플 1) CREATE
 * <pre>{@code
 * {
 *   "operation": "CREATE",
 *   "orderType": "ORDER_CRUD",
 *   "payload": {
 *     "id": 1012345678918,
 *     "orderId": 1012345678918,
 *     "orderNumber": "ORD-1012345678918",
 *
 *     "userId": 100000001,
 *     "userNumber": "U-100000001",
 *     "orderPrice": 130000,
 *     "deleteYn": false,
 *     "version": null,
 *
 *     "createdUserId": 2001,
 *     "createdUserType": "SYSTEM",
 *     "createdDatetime": "2025-09-21T06:00:00",
 *
 *     "modifiedUserId": 2001,
 *     "modifiedUserType": "SYSTEM",
 *     "modifiedDatetime": "2025-09-21T06:00:00",
 *
 *     "publishedTimestamp": 1758434400000
 *   }
 * }
 * }</pre>
 * <p>
 * ### 의도
 * - 신규 데이터 적재. Worker는 JDBC 벌크 INSERT + (증폭 INSERT) + 외부 동기화(Dynamo/Redis)를 수행.
 * <p>
 * <hr/>
 * <p>
 * ## 샘플 2) UPDATE
 * <pre>{@code
 * {
 *   "operation": "UPDATE",
 *   "orderType": "ORDER_CRUD",
 *   "payload": {
 *     "id": 1012345678918,
 *     "orderId": 1012345678918,
 *     "orderNumber": "ORD-1012345678918",
 *
 *     "userId": 100000001,
 *     "userNumber": "U-100000001",
 *     "orderPrice": 150000,
 *     "deleteYn": false,
 *     "version": null,
 *
 *     "createdUserId": 2001,
 *     "createdUserType": "SYSTEM",
 *     "createdDatetime": "2025-09-21T06:00:00",
 *
 *     "modifiedUserId": 2001,
 *     "modifiedUserType": "SYSTEM",
 *     "modifiedDatetime": "2025-09-22T06:00:00",
 *
 *     "publishedTimestamp": 1758434400000
 *   }
 * }
 * }</pre>
 * <p>
 * ### 의도
 * - 기존 데이터 갱신. Worker는 JDBC 벌크 UPDATE + (증폭 UPDATE) + 외부 동기화(Dynamo/Redis)를 수행.
 * <p>
 * <hr/>
 * <p>
 * ## 샘플 3) DELETE
 * <pre>{@code
 * {
 *   "operation": "DELETE",
 *   "orderType": "ORDER_CRUD",
 *   "payload": {
 *     "id": 1012345678918,
 *     "orderId": 1012345678918,
 *     "orderNumber": "ORD-1012345678918",
 *
 *     "userId": null,
 *     "userNumber": null,
 *     "orderPrice": null,
 *     "deleteYn": true,
 *     "version": null,
 *
 *     "createdUserId": null,
 *     "createdUserType": null,
 *     "createdDatetime": null,
 *
 *     "modifiedUserId": 2001,
 *     "modifiedUserType": "SYSTEM",
 *     "modifiedDatetime": "2025-09-23T16:00:00",
 *
 *     "publishedTimestamp": null
 *   }
 * }
 * }</pre>
 * <p>
 * ### 의도
 * - 논리 삭제 플래그(<code>deleteYn=true</code>) 기준의 삭제 처리.
 * - Worker는 원본 삭제 + (증폭 삭제) 후 외부 동기화(Dynamo 파티션/정렬키 기준 삭제, Redis 키 삭제) 수행.
 * <p>
 * <hr/>
 * <p>
 * ## 빠른 테스트 팁
 * <pre>{@code
 * # (로컬) 콘솔 프로듀서 실행 예시
 * kafka-console-producer.sh --bootstrap-server 127.0.0.1:29092 --topic local-order-crud
 *
 * # 위 창에 샘플 JSON을 그대로 붙여넣고 Enter
 * }</pre>
 *
 * <p><b>주의</b>: 계약(Contract)의 필수 필드(<code>operation</code>, <code>payload.orderId</code>)가 누락/무효이면
 * 파사드에서 DLQ 전송 또는 스킵될 수 있습니다.</p>
 */

package org.example.order.worker.listener.order;
