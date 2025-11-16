package org.codeexpert.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent extends BaseEvent {

    @Builder.Default
    private String commandType = "PaymentProcessedEvent";

    private String orderId;

    private String transactionId;

    private String correlationId;
}
