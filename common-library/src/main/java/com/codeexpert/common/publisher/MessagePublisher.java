package com.codeexpert.common.publisher;

import com.codeexpert.common.event.DomainEvent; // Changed import

public interface MessagePublisher { // Changed interface name
    <T extends DomainEvent> void publish(String topic, String key, T message); // Changed T extends BaseCommand to T extends DomainEvent, and command to message
}
