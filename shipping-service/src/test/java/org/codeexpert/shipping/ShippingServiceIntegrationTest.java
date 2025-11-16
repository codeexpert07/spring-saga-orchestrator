package org.codeexpert.shipping;

import org.codeexpert.common.command.CreateShipmentCommand;
import org.codeexpert.common.constant.KafkaTopics;
import org.codeexpert.common.event.ShipmentCreatedEvent;
import org.codeexpert.common.model.OrderItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        topics = {
                KafkaTopics.SHIPPING_COMMANDS,
                KafkaTopics.SHIPPING_EVENTS
        },
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9094",
                "port=9094",
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
        "spring.kafka.consumer.group-id=shipping-service-test-group",

        // Kafka Producer Serialization
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",

        // Kafka Consumer Deserialization
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=*"
})
@Import(TestConfig.class)
class ShippingServiceIntegrationTest {

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

            consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

            // Create consumer factory with explicit deserializers
            DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(
                    consumerProps,
                    new StringDeserializer(),
                    new StringDeserializer()
            );

            testConsumer = cf.createConsumer();
            testConsumer.subscribe(Collections.singletonList(KafkaTopics.SHIPPING_EVENTS));

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
    void shouldProcessCreateShipmentCommandAndPublishShipmentCreatedEvent() throws Exception {
        // Wait for listeners to be ready
        waitForKafkaListeners();

        // Given
        String orderId = UUID.randomUUID().toString();
        CreateShipmentCommand command = CreateShipmentCommand.builder()
                .orderId(orderId)
                .items(Collections.singletonList(OrderItem.builder().productId("1").price(BigDecimal.ONE).quantity(1).build())) // Assuming some items
                .correlationId(orderId)
                .build();

        // When
        kafkaTemplate.send(KafkaTopics.SHIPPING_COMMANDS, command.getOrderId(), command)
                .get(10, TimeUnit.SECONDS);
        System.out.println("Sent CreateShipmentCommand: " + command);

        // Then
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            ConsumerRecord<String, String> receivedRecord = KafkaTestUtils.getSingleRecord(
                    testConsumer,
                    KafkaTopics.SHIPPING_EVENTS,
                    Duration.ofMillis(10000)
            );

            assertNotNull(receivedRecord, "No record received from Kafka");
            assertNotNull(receivedRecord.value(), "Received record value is null");
            
            // Deserialize the JSON string to ShipmentCreatedEvent
            ShipmentCreatedEvent event = objectMapper.readValue(
                    receivedRecord.value(), 
                    ShipmentCreatedEvent.class
            );
            
            System.out.println("Received ShipmentCreatedEvent: " + event);
            assertEquals(orderId, event.getOrderId());
            assertEquals("SUCCESS", event.getStatus());
            assertNotNull(event.getShipmentId());
        });
    }
}
