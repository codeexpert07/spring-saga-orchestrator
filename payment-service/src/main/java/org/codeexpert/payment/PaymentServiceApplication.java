package org.codeexpert.payment;

import org.codeexpert.common.constant.KafkaTopics;
import org.codeexpert.common.listener.DomainEventListener;
import org.codeexpert.common.listener.KafkaListenerRegistrar;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication( scanBasePackages = {
        "org.codeexpert"
})
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
                KafkaTopics.PAYMENT_COMMANDS, groupId, paymentCommandListener);
    }
}
