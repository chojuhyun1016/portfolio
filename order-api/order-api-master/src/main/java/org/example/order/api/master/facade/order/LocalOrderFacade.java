package org.example.order.api.master.facade.order;

import org.example.order.api.master.dto.order.LocalOrderPublishRequest;
import org.example.order.core.application.order.dto.query.LocalOrderQuery;
import org.example.order.core.application.order.dto.view.LocalOrderView;

/**
 * Order Facade
 * - 메시지 발행/전송
 * - 단건 조회(가공 포함)
 */
public interface LocalOrderFacade {

    void sendOrderMessage(LocalOrderPublishRequest request);

    LocalOrderView findById(LocalOrderQuery query);
}
