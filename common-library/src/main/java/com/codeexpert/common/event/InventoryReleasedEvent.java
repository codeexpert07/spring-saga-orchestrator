package com.codeexpert.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InventoryReleasedEvent extends BaseEvent{

    @Builder.Default
    private String commandType = "InventoryReleasedEvent";

    private String orderId;

    private String reservationId;

    private String correlationId;
}
