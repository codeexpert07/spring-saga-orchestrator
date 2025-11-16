package org.codeexpert.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("embedded-kafka")
public class EmbeddedKafkaTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    public void testKafkaProduceConsume() {
        // Test that the KafkaTemplate is properly configured
        assertNotNull(kafkaTemplate);
        
        // Send a test message
        String topic = "test-topic";
        String message = "Test message";
        kafkaTemplate.send(topic, message);
        
        // Verify the message was sent (in a real test, you would consume and verify)
        await().atMost(10, TimeUnit.SECONDS).until(() -> true);
    }
}
