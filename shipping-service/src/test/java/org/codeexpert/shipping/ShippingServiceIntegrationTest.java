package org.codeexpert.shipping;

import com.codeexpert.common.command.CreateShipmentCommand;
import com.codeexpert.common.constant.KafkaTopics;
import com.codeexpert.common.event.ShipmentCreatedEvent;
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

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class ShippingServiceIntegrationTest {

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
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", kafka.getBootstrapServers());
        DefaultKafkaConsumerFactory<String, Object> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        testConsumer = cf.createConsumer();
        testConsumer.subscribe(Collections.singletonList(KafkaTopics.SHIPPING_EVENTS));
        testConsumer.poll(Duration.ofSeconds(1)); // Ensure consumer is assigned partitions
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
    void shouldProcessCreateShipmentCommandAndPublishShipmentCreatedEvent() throws Exception {
        // Given
        String orderId = UUID.randomUUID().toString();
        CreateShipmentCommand command = CreateShipmentCommand.builder()
                .orderId(orderId)
                .items(Collections.singletonMap("item1", 1)) // Assuming some items
                .correlationId(orderId)
                .build();

        // When
        kafkaTemplate.send(KafkaTopics.SHIPPING_COMMANDS, command.getOrderId(), command).get(10, TimeUnit.SECONDS);
        System.out.println("Sent CreateShipmentCommand: " + command);

        // Then
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            ConsumerRecord<String, Object> receivedRecord = KafkaTestUtils.getSingleRecord(testConsumer, KafkaTopics.SHIPPING_EVENTS, Duration.ofMillis(10000));
            assertNotNull(receivedRecord);
            Object value = receivedRecord.value();
            assertNotNull(value);
            assertTrue(value instanceof ShipmentCreatedEvent);
            ShipmentCreatedEvent event = (ShipmentCreatedEvent) value;
            System.out.println("Received ShipmentCreatedEvent: " + event);

            assertEquals(orderId, event.getOrderId());
            assertEquals("SUCCESS", event.getStatus()); // Assuming success by default
            assertNotNull(event.getShipmentId());
        });
    }
}
