package org.codeexpert.common.command;

import org.codeexpert.common.model.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReserveInventoryCommand extends BaseCommand{
    @Builder.Default
    private String commandType = "ReserveInventoryCommand";

    private List<OrderItem> items;

}
