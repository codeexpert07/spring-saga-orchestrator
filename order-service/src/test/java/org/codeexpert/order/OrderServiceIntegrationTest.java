package org.codeexpert.order;

import org.junit.jupiter.api.AfterEach;
import org.codeexpert.common.command.ProcessPaymentCommand;
import org.codeexpert.common.constant.KafkaTopics;
import org.codeexpert.common.event.PaymentProcessedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.codeexpert.order.model.OrderEvent;
import org.codeexpert.order.model.OrderState;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        topics = {
                KafkaTopics.PAYMENT_COMMANDS,
                KafkaTopics.PAYMENT_EVENTS,
                KafkaTopics.INVENTORY_COMMANDS,
                KafkaTopics.INVENTORY_EVENTS,
                KafkaTopics.SHIPPING_COMMANDS,
                KafkaTopics.SHIPPING_EVENTS
        },
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9092",
                "port=9092",
                "auto.create.topics.enable=true"
        }
)
@TestPropertySource(properties = {
        // H2 Database Configuration
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.h2.console.enabled=true",

        // Kafka Configuration
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.group-id=order-service-test-group",

        // Kafka Producer Serialization
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",

        // Kafka Consumer Deserialization
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=*"
})
@Import(TestConfig.class)
class OrderServiceIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private StateMachineFactory<OrderState, OrderEvent> stateMachineFactory;
    
    private StateMachine<OrderState, OrderEvent> stateMachine;

    private static Consumer<String, String> testConsumer;

    @BeforeAll
    static void setup(@Autowired EmbeddedKafkaBroker embeddedKafkaBroker) {
        try {
            // Build consumer properties for embedded Kafka
            Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                    "test-consumer-group",
                    "true",
                    embeddedKafkaBroker
            );

            consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

            // Create consumer factory with explicit deserializers
            DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(
                    consumerProps,
                    new StringDeserializer(),
                    new StringDeserializer()
            );

            testConsumer = cf.createConsumer();
            testConsumer.subscribe(Collections.singletonList(KafkaTopics.PAYMENT_EVENTS));

            // Initial poll to ensure consumer is assigned partitions
            testConsumer.poll(Duration.ofSeconds(1));

            System.out.println("Test consumer setup completed successfully");
        } catch (Exception e) {
            System.err.println("Error setting up test consumer: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @AfterEach
    void tearDown() {
        if (testConsumer != null) {
            testConsumer.close();
            testConsumer = null;
        }
        if (stateMachine != null && stateMachine.isComplete()) {
            stateMachine.stop();
            stateMachine = null;
        }
    }

    /**
     * Wait for all Kafka listeners to be running before executing tests
     */
    private void waitForKafkaListeners() {
        await().atMost(Duration.ofSeconds(30)).until(() -> {
            boolean allRunning = kafkaListenerEndpointRegistry.getListenerContainers()
                    .stream()
                    .allMatch(container -> container.isRunning());
            if (allRunning) {
                System.out.println("All Kafka listeners are running");
            }
            return allRunning;
        });
    }

    @Test
    void contextLoads() {
        assertNotNull(kafkaTemplate);
        assertNotNull(embeddedKafkaBroker);
        assertNotNull(kafkaListenerEndpointRegistry);
    }

    @Test
    void shouldProcessPaymentCommandAndPublishPaymentProcessedEvent() throws Exception {
        // Wait for Kafka listeners to be ready
        waitForKafkaListeners();

        // Given
        String orderId = UUID.randomUUID().toString();
        String customerId = UUID.randomUUID().toString();
        BigDecimal amount = new BigDecimal("100.00");
        
        ProcessPaymentCommand command = ProcessPaymentCommand.builder()
                .orderId(orderId)
                .customerId(customerId)
                .amount(amount)
                .correlationId(orderId)
                .build();

        System.out.println("Sending ProcessPaymentCommand: " + command);

        // When
        kafkaTemplate.send(KafkaTopics.PAYMENT_COMMANDS, command.getOrderId(), command).get(10, TimeUnit.SECONDS);

        // Then
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            // Poll for the event with a timeout
            ConsumerRecord<String, String> receivedRecord = KafkaTestUtils.getSingleRecord(
                    testConsumer, 
                    KafkaTopics.PAYMENT_EVENTS,
                    Duration.ofSeconds(10)
            );
            
            assertNotNull(receivedRecord, "No message received on topic: " + KafkaTopics.PAYMENT_EVENTS);
            
            // Deserialize the JSON string to PaymentProcessedEvent
            PaymentProcessedEvent event = objectMapper.readValue(
                    receivedRecord.value(), 
                    PaymentProcessedEvent.class
            );
            
            System.out.println("Received PaymentProcessedEvent: " + event);
            
            // Assertions
            assertNotNull(event, "Event should not be null");
            assertEquals(orderId, event.getOrderId(), "Order ID should match");
            assertEquals("SUCCESS", event.getStatus(), "Status should be SUCCESS");
            assertNotNull(event.getTransactionId(), "Transaction ID should not be null");
        });
    }
    
    @Test
    void shouldHandleFullOrderWorkflow() throws Exception {
        // Wait for Kafka listeners to be ready
        waitForKafkaListeners();

        // Given - Create a test order
        String orderId = UUID.randomUUID().toString();
        String customerId = UUID.randomUUID().toString();
        BigDecimal amount = new BigDecimal("150.00");
        
        // Create a test consumer for inventory events
        Map<String, Object> inventoryConsumerProps = KafkaTestUtils.consumerProps(
                "test-inventory-consumer", 
                "true", 
                embeddedKafkaBroker
        );
        inventoryConsumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        inventoryConsumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        
        Consumer<String, String> inventoryConsumer = new DefaultKafkaConsumerFactory<>(
                inventoryConsumerProps,
                new StringDeserializer(),
                new StringDeserializer()
        ).createConsumer();
        
        try {
            // Subscribe to inventory commands topic
            inventoryConsumer.subscribe(Collections.singletonList(KafkaTopics.INVENTORY_COMMANDS));
            // Initial poll to subscribe
            inventoryConsumer.poll(Duration.ofSeconds(1));

            // When - Send the payment command to start the saga
            ProcessPaymentCommand paymentCommand = ProcessPaymentCommand.builder()
                    .orderId(orderId)
                    .customerId(customerId)
                    .amount(amount)
                    .correlationId(orderId)
                    .build();

            System.out.println("Sending ProcessPaymentCommand: " + paymentCommand);
            kafkaTemplate.send(KafkaTopics.PAYMENT_COMMANDS, paymentCommand.getOrderId(), paymentCommand)
                    .get(10, TimeUnit.SECONDS);

            // Then - Verify inventory command was sent
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                ConsumerRecord<String, String> inventoryRecord = KafkaTestUtils.getSingleRecord(
                        inventoryConsumer,
                        KafkaTopics.INVENTORY_COMMANDS,
                        Duration.ofSeconds(10)
                );
                
                assertNotNull(inventoryRecord, "No inventory command was sent");
                System.out.println("Received inventory command: " + inventoryRecord.value());
                
                // Here you would typically deserialize and verify the inventory command
                // and then simulate a response by publishing an InventoryReservedEvent
            });
            
            // Note: In a real test, you would continue the saga by publishing the next event
            // and verifying the subsequent steps in the workflow
            
        } finally {
            inventoryConsumer.close();
        }
    }
}
