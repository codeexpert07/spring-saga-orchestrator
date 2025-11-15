package com.codeexpert.common.listener;

import com.codeexpert.common.command.BaseCommand;
import com.codeexpert.common.event.BaseEvent;
import com.codeexpert.common.event.DomainEvent;
import org.apache.kafka.common.errors.SerializationException;

public interface EventSerializer {

    DomainEvent fromString(String source) throws SerializationException;

    String toString(DomainEvent command) throws SerializationException;
}
