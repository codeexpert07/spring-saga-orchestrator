package org.codeexpert.payment;

import com.codeexpert.common.command.ProcessPaymentCommand;
import com.codeexpert.common.constant.KafkaTopics;
import com.codeexpert.common.event.DomainEvent;
import com.codeexpert.common.event.PaymentProcessedEvent;
import com.codeexpert.common.listener.JsonEventSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
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
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        topics = {
                KafkaTopics.PAYMENT_COMMANDS,
                KafkaTopics.PAYMENT_EVENTS
        },
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9093",
                "port=9093",
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
        "spring.kafka.consumer.group-id=payment-service-test-group",

        // Kafka Producer Serialization
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",

        // Kafka Consumer Deserialization
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=*"
})
@Import(TestConfig.class)
class PaymentServiceIntegrationTest {

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
    void shouldProcessPaymentCommandAndPublishPaymentProcessedEvent() throws Exception {
        // Wait for listeners to be ready
        waitForKafkaListeners();

        // Given
        String orderId = UUID.randomUUID().toString();
        String customerId = UUID.randomUUID().toString();
        BigDecimal amount = BigDecimal.valueOf(150.75);
        
        ProcessPaymentCommand command = ProcessPaymentCommand.builder()
                .orderId(orderId)
                .customerId(customerId)
                .amount(amount)
                .correlationId(orderId)
                .build();

        // When
        kafkaTemplate.send(KafkaTopics.PAYMENT_COMMANDS, command.getOrderId(), command)
                .get(10, TimeUnit.SECONDS);
        System.out.println("Sent ProcessPaymentCommand: " + command);

        // Then
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            ConsumerRecord<String, String> receivedRecord = KafkaTestUtils.getSingleRecord(
                    testConsumer,
                    KafkaTopics.PAYMENT_EVENTS,
                    Duration.ofMillis(10000)
            );

            assertNotNull(receivedRecord, "No record received from Kafka");
            assertNotNull(receivedRecord.value(), "Received record value is null");
            
            // Deserialize the JSON string to PaymentProcessedEvent
            PaymentProcessedEvent event = objectMapper.readValue(
                    receivedRecord.value(), 
                    PaymentProcessedEvent.class
            );
            
            System.out.println("Received PaymentProcessedEvent: " + event);

            assertEquals(orderId, event.getOrderId(), "Order ID mismatch");
            assertEquals("SUCCESS", event.getStatus(), "Payment status should be SUCCESS");
            assertNotNull(event.getTransactionId(), "Transaction ID should not be null");
            assertEquals(orderId, event.getCorrelationId(), "Correlation ID should match order ID");
        });
    }
}
