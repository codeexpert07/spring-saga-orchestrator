package com.codeexpert.common.command;

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
public class ReleaseInventoryCommand extends BaseCommand {

    @Builder.Default
    private String commandType = "ReleaseInventoryCommand";
    private String reservationId;
}
