package org.codeexpert.payment;

import com.codeexpert.common.command.ProcessPaymentCommand;
import com.codeexpert.common.constant.KafkaTopics;
import com.codeexpert.common.listener.DomainEventListener;
import com.codeexpert.common.listener.KafkaListenerRegistrar;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentServiceApplication {

    private final KafkaListenerRegistrar kafkaListenerRegistrar;
    private final DomainEventListener paymentCommandListener;

    @Autowired
    public PaymentServiceApplication(KafkaListenerRegistrar kafkaListenerRegistrar, DomainEventListener paymentCommandListener) {
        this.kafkaListenerRegistrar = kafkaListenerRegistrar;
        this.paymentCommandListener = paymentCommandListener;
    }

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

    @PostConstruct
    public void registerKafkaListeners() {
        String groupId = "payment-service-group"; // Define group ID here or in properties

        kafkaListenerRegistrar.registerListener(
                KafkaTopics.PAYMENT_COMMANDS, groupId, paymentCommandListener, ProcessPaymentCommand.class);
    }
}
