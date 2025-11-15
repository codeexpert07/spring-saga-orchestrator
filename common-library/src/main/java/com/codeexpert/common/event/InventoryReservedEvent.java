package com.codeexpert.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservedEvent extends BaseEvent {

    @Builder.Default
    private String commandType = "InventoryReservedEvent";

    private String orderId;

    private String reservationId;

    private String correlationId;
}
