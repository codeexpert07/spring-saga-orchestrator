package org.codeexpert.inventory;

import com.codeexpert.common.command.ReleaseInventoryCommand;
import com.codeexpert.common.command.ReserveInventoryCommand;
import com.codeexpert.common.constant.KafkaTopics;
import com.codeexpert.common.event.InventoryReleasedEvent;
import com.codeexpert.common.event.InventoryReservedEvent;
import com.codeexpert.common.listener.KafkaListenerRegistrar;
import com.codeexpert.common.model.OrderItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
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
import java.util.List;
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
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:13")
    ).withStartupTimeout(Duration.ofMinutes(2));

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    ).withStartupTimeout(Duration.ofMinutes(2));

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KafkaListenerRegistrar kafkaListenerRegistrar;

    private static Consumer<String, Object> testConsumer;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL properties
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // HikariCP configuration for CI environment
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "1");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "60000");
        registry.add("spring.datasource.hikari.initialization-fail-timeout", () -> "60000");
        registry.add("spring.datasource.hikari.validation-timeout", () -> "5000");

        // JPA properties
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");

        // Kafka properties
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id", () -> "inventory-service-group");
    }

    @BeforeAll
    static void setup() {
        try {
            // Manually build consumer properties to avoid parameter order issues
            Map<String, Object> consumerProps = new java.util.HashMap<>();
            consumerProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    kafka.getBootstrapServers());
            consumerProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, "test-group");
            consumerProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
            consumerProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            consumerProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    org.apache.kafka.common.serialization.StringDeserializer.class);
            consumerProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    JsonDeserializer.class);
            consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
            consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
            consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class.getName());

            // Create consumer factory with explicit deserializers
            DefaultKafkaConsumerFactory<String, Object> cf = new DefaultKafkaConsumerFactory<>(
                    consumerProps,
                    new org.apache.kafka.common.serialization.StringDeserializer(),
                    new JsonDeserializer<>(Object.class).trustedPackages("*")
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

    @Test
    void contextLoads() {
        assertNotNull(kafkaTemplate);
        assertNotNull(objectMapper);
        System.out.println("Context loaded successfully");
    }

    @Test
    void shouldProcessReserveInventoryCommandAndPublishInventoryReservedEvent() throws Exception {
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
            ConsumerRecord<String, Object> receivedRecord = KafkaTestUtils.getSingleRecord(
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
            ConsumerRecord<String, Object> receivedRecord = KafkaTestUtils.getSingleRecord(
                    testConsumer,
                    KafkaTopics.INVENTORY_EVENTS,
                    Duration.ofMillis(10000)
            );

            assertNotNull(receivedRecord, "No record received from Kafka");
            Object value = receivedRecord.value();
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