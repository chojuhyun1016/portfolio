package org.example.order.api.master.facade.order.impl;

import lombok.RequiredArgsConstructor;
import org.example.order.api.master.dto.order.LocalOrderPublishRequest;
import org.example.order.api.master.facade.order.LocalOrderFacade;
import org.example.order.api.master.mapper.order.LocalOrderRequestMapper;
import org.example.order.api.master.service.order.LocalOrderService;
import org.example.order.core.application.order.dto.command.LocalOrderCommand;
import org.example.order.core.application.order.dto.query.LocalOrderQuery;
import org.example.order.core.application.order.dto.view.LocalOrderView;
import org.springframework.stereotype.Component;

/**
 * 파사드 구현
 * - 메시지 발행/전송
 * - 조회(가공 포함) 위임
 */
@RequiredArgsConstructor
@Component
public class LocalOrderFacadeImpl implements LocalOrderFacade {

    private final LocalOrderService localOrderService;
    private final LocalOrderRequestMapper localOrderRequestMapper;

    @Override
    public void sendOrderMessage(LocalOrderPublishRequest request) {
        LocalOrderCommand command = localOrderRequestMapper.toCommand(request);

        localOrderService.sendMessage(command);
    }

    @Override
    public LocalOrderView findById(LocalOrderQuery query) {
        return localOrderService.findById(query);
    }
}
