package org.codeexpert.common.publisher;

import org.codeexpert.common.event.DomainEvent; // Changed import

public interface MessagePublisher { // Changed interface name
    <T extends DomainEvent> void publish(String topic, String key, T message); // Changed T extends BaseCommand to T extends DomainEvent, and command to message
}
