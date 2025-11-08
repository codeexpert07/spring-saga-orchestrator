package com.codeexpert.common.command;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class RefundPaymentCommand extends BaseCommand {
    private String paymentTransactionId;
}
