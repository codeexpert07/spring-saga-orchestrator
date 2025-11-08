package com.codeexpert.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class InventoryReleasedEvent extends BaseEvent{

    private String orderId;

    private String reservationId;

    private String correlationId;
}
