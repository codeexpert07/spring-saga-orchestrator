package org.codeexpert.inventory;

import com.codeexpert.common.command.ReleaseInventoryCommand;
import com.codeexpert.common.command.ReserveInventoryCommand;
import com.codeexpert.common.constant.KafkaTopics;
import com.codeexpert.common.event.InventoryReleasedEvent;
import com.codeexpert.common.event.InventoryReservedEvent;
import com.codeexpert.common.model.OrderItem; // Added import
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List; // Added import
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class InventoryServiceIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:13"));

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static Consumer<String, Object> testConsumer;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    }

    @BeforeAll
    static void setup() {
        // Wait for Kafka to be ready
        kafka.start();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", kafka.getBootstrapServers());
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.springframework.kafka.support.serializer.JsonDeserializer");
        consumerProps.put("spring.json.trusted.packages", "*");

        DefaultKafkaConsumerFactory<String, Object> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        testConsumer = cf.createConsumer();
        testConsumer.subscribe(Collections.singletonList(KafkaTopics.INVENTORY_EVENTS));
        testConsumer.poll(Duration.ofSeconds(1));
    }

    @AfterAll
    static void tearDown() {
        if (testConsumer != null) {
            testConsumer.close();
        }
    }

    @Test
    void contextLoads() {
        assertNotNull(kafkaTemplate);
    }

    @Test
    void shouldProcessReserveInventoryCommandAndPublishInventoryReservedEvent() throws Exception {
        // Given
        String orderId = UUID.randomUUID().toString();
        OrderItem orderItem = OrderItem.builder() // Create OrderItem
                .productId("product1")
                .quantity(1)
                .price(BigDecimal.valueOf(10.00))
                .build();
        List<OrderItem> items = Collections.singletonList(orderItem); // Create List of OrderItem

        ReserveInventoryCommand command = ReserveInventoryCommand.builder()
                .orderId(orderId)
                .items(items) // Pass the List of OrderItem
                .correlationId(orderId)
                .build();

        // When
        kafkaTemplate.send(KafkaTopics.INVENTORY_COMMANDS, command.getOrderId(), command).get(10, TimeUnit.SECONDS);
        System.out.println("Sent ReserveInventoryCommand: " + command);

        // Then
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            ConsumerRecord<String, Object> receivedRecord = KafkaTestUtils.getSingleRecord(testConsumer, KafkaTopics.INVENTORY_EVENTS, Duration.ofMillis(10000));
            assertNotNull(receivedRecord);
            Object value = receivedRecord.value();
            assertNotNull(value);
            assertTrue(value instanceof InventoryReservedEvent);
            InventoryReservedEvent event = (InventoryReservedEvent) value;
            System.out.println("Received InventoryReservedEvent: " + event);

            assertEquals(orderId, event.getOrderId());
            assertEquals("SUCCESS", event.getStatus());
            assertNotNull(event.getReservationId());
        });
    }

    @Test
    void shouldProcessReleaseInventoryCommandAndPublishInventoryReleasedEvent() throws Exception {
        // Given
        String orderId = UUID.randomUUID().toString();
        String reservationId = UUID.randomUUID().toString();
        ReleaseInventoryCommand command = ReleaseInventoryCommand.builder()
                .orderId(orderId)
                .reservationId(reservationId)
                .correlationId(orderId)
                .build();

        // When
        kafkaTemplate.send(KafkaTopics.INVENTORY_COMMANDS, command.getOrderId(), command).get(10, TimeUnit.SECONDS);
        System.out.println("Sent ReleaseInventoryCommand: " + command);

        // Then
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            ConsumerRecord<String, Object> receivedRecord = KafkaTestUtils.getSingleRecord(testConsumer, KafkaTopics.INVENTORY_EVENTS, Duration.ofMillis(10000));
            assertNotNull(receivedRecord);
            Object value = receivedRecord.value();
            assertNotNull(value);
            assertTrue(value instanceof InventoryReleasedEvent);
            InventoryReleasedEvent event = (InventoryReleasedEvent) value;
            System.out.println("Received InventoryReleasedEvent: " + event);

            assertEquals(orderId, event.getOrderId());
            assertEquals("SUCCESS", event.getStatus()); // Assuming success by default
            assertEquals(reservationId, event.getReservationId());
        });
    }
}
