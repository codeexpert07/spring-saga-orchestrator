package com.codeexpert.common.command;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ProcessPaymentCommand extends BaseCommand {

    private String customerId;

    private BigDecimal amount;
}
