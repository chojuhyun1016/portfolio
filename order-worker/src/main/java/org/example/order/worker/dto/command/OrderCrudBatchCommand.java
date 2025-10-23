package org.example.order.worker.dto.command;

import org.example.order.contract.shared.op.Operation;
import org.example.order.core.application.order.dto.sync.LocalOrderSync;

import java.util.List;

/**
 * CRUD 배치 커맨드 공통 인터페이스 (dto 계층)
 * - Listener/Facade에서 연산별 배치를 명시적으로 전달하기 위한 계약
 * - items()는 불변 리스트로 전달하는 것을 권장(List.copyOf)
 */
public interface OrderCrudBatchCommand {
    Operation operation();

    List<LocalOrderSync> items();
}
