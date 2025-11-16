package org.codeexpert.common.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentRefundedEvent {
    private String orderId;
    private String transactionId;
    private String correlationId;
}
