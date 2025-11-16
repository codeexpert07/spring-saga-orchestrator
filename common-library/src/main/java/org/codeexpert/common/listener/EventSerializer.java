package org.codeexpert.common.listener;

import org.codeexpert.common.event.DomainEvent;
import org.apache.kafka.common.errors.SerializationException;

public interface EventSerializer {

    DomainEvent fromString(String source) throws SerializationException;

    String toString(DomainEvent command) throws SerializationException;
}
