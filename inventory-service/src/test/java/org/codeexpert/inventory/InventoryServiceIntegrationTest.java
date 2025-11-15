package org.codeexpert.inventory;

import com.codeexpert.common.command.BaseCommand;
import com.codeexpert.common.command.ReleaseInventoryCommand;
import com.codeexpert.common.command.ReserveInventoryCommand;
import com.codeexpert.common.constant.KafkaTopics;
import com.codeexpert.common.event.BaseEvent;
import com.codeexpert.common.event.DomainEvent;
import com.codeexpert.common.event.InventoryReleasedEvent;
import com.codeexpert.common.event.InventoryReservedEvent;
import com.codeexpert.common.listener.JsonEventSerializer;
import com.codeexpert.common.model.OrderItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterAll;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        topics = {
                KafkaTopics.INVENTORY_COMMANDS,
                KafkaTopics.INVENTORY_EVENTS
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
        "spring.h2.console.enabled=false",

        // Kafka Configuration
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.group-id=inventory-service-test-group",

        // Kafka Producer Serialization
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",

        // Kafka Consumer Deserialization
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
})
@Import(TestConfig.class)
class InventoryServiceIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

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

            consumerProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

            // Create consumer factory with explicit deserializers
            DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(
                    consumerProps,
                    new org.apache.kafka.common.serialization.StringDeserializer(),
                    new org.apache.kafka.common.serialization.StringDeserializer()
            );

            testConsumer = cf.createConsumer();
            testConsumer.subscribe(Collections.singletonList(KafkaTopics.INVENTORY_EVENTS));

            // Initial poll to ensure consumer is assigned partitions
            testConsumer.poll(Duration.ofSeconds(1));

            System.out.println("Test consumer setup completed successfully");
        } catch (Exception e) {
            System.err.println("Error setting up test consumer: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @AfterAll
    static void tearDown() {
        if (testConsumer != null) {
            try {
                testConsumer.close();
                System.out.println("Test consumer closed successfully");
            } catch (Exception e) {
                System.err.println("Error closing test consumer: " + e.getMessage());
            }
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
        assertNotNull(objectMapper);
        assertNotNull(embeddedKafkaBroker);
        System.out.println("Context loaded successfully with embedded Kafka");
    }

    @Test
    void shouldProcessReserveInventoryCommandAndPublishInventoryReservedEvent() throws Exception {
        // Wait for listeners to be ready
        waitForKafkaListeners();

        // Given
        String orderId = UUID.randomUUID().toString();
        OrderItem orderItem = OrderItem.builder()
                .productId("product1")
                .quantity(1)
                .price(BigDecimal.valueOf(10.00))
                .build();
        List<OrderItem> items = Collections.singletonList(orderItem);

        ReserveInventoryCommand command = ReserveInventoryCommand.builder()
                .orderId(orderId)
                .items(items)
                .correlationId(orderId)
                .build();

        // When
        kafkaTemplate.send(KafkaTopics.INVENTORY_COMMANDS, command.getOrderId(), command)
                .get(10, TimeUnit.SECONDS);
        System.out.println("Sent ReserveInventoryCommand: " + command);

        // Then
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            ConsumerRecord<String, String> receivedRecord = KafkaTestUtils.getSingleRecord(
                    testConsumer,
                    KafkaTopics.INVENTORY_EVENTS,
                    Duration.ofMillis(10000)
            );

            assertNotNull(receivedRecord, "No record received from Kafka");
            Object value = receivedRecord.value();
            assertNotNull(value, "Record value is null");
            assertTrue(value instanceof InventoryReservedEvent,
                    "Expected InventoryReservedEvent but got: " + value.getClass().getName());

            InventoryReservedEvent event = (InventoryReservedEvent) value;
            System.out.println("Received InventoryReservedEvent: " + event);

            assertEquals(orderId, event.getOrderId());
            assertEquals("SUCCESS", event.getStatus());
            assertNotNull(event.getReservationId());
        });
    }

    @Test
    void shouldProcessReleaseInventoryCommandAndPublishInventoryReleasedEvent() throws Exception {
        // Wait for listeners to be ready
        waitForKafkaListeners();

        // Given
        String orderId = UUID.randomUUID().toString();
        String reservationId = UUID.randomUUID().toString();
        ReleaseInventoryCommand command = ReleaseInventoryCommand.builder()
                .orderId(orderId)
                .reservationId(reservationId)
                .correlationId(orderId)
                .build();

        // When
        kafkaTemplate.send(KafkaTopics.INVENTORY_COMMANDS, command.getOrderId(), command)
                .get(10, TimeUnit.SECONDS);
        System.out.println("Sent ReleaseInventoryCommand: " + command);

        // Then
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            ConsumerRecord<String, String> receivedRecord = KafkaTestUtils.getSingleRecord(
                    testConsumer,
                    KafkaTopics.INVENTORY_EVENTS,
                    Duration.ofMillis(10000)
            );

            assertNotNull(receivedRecord, "No record received from Kafka");
            DomainEvent value = new JsonEventSerializer().fromString(receivedRecord.value());
            assertNotNull(value, "Record value is null");
            assertTrue(value instanceof InventoryReleasedEvent,
                    "Expected InventoryReleasedEvent but got: " + value.getClass().getName());

            InventoryReleasedEvent event = (InventoryReleasedEvent) value;
            System.out.println("Received InventoryReleasedEvent: " + event);

            assertEquals(orderId, event.getOrderId());
            assertEquals("SUCCESS", event.getStatus());
            assertEquals(reservationId, event.getReservationId());
        });
    }
}