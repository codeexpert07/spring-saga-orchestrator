package com.codeexpert.common.command;

import com.codeexpert.common.model.OrderItem;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CreateShipmentCommand extends BaseCommand {

    private List<OrderItem> items;
}
