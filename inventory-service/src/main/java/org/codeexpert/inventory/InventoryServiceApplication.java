package org.codeexpert.inventory;

import com.codeexpert.common.command.ReleaseInventoryCommand;
import com.codeexpert.common.command.ReserveInventoryCommand;
import com.codeexpert.common.constant.KafkaTopics;
import com.codeexpert.common.listener.DomainEventListener;
import com.codeexpert.common.listener.KafkaListenerRegistrar;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.statemachine.config.EnableStateMachine;

@SpringBootApplication
@EnableStateMachine
public class InventoryServiceApplication {

    private final KafkaListenerRegistrar kafkaListenerRegistrar;
    private final DomainEventListener inventoryCommandListener;

    @Autowired
    public InventoryServiceApplication(KafkaListenerRegistrar kafkaListenerRegistrar, DomainEventListener inventoryCommandListener) {
        this.kafkaListenerRegistrar = kafkaListenerRegistrar;
        this.inventoryCommandListener = inventoryCommandListener;
    }

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }

    @PostConstruct
    public void registerKafkaListeners() {
        String groupId = "inventory-service-group"; // Define group ID here or in properties

        kafkaListenerRegistrar.registerListener(
                KafkaTopics.INVENTORY_COMMANDS, groupId, inventoryCommandListener, ReserveInventoryCommand.class);
        kafkaListenerRegistrar.registerListener(
                KafkaTopics.INVENTORY_COMMANDS, groupId, inventoryCommandListener, ReleaseInventoryCommand.class);
    }
}
