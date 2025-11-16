package org.codeexpert.common.listener;

import org.codeexpert.common.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonEventSerializer implements EventSerializer {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public DomainEvent fromString(String source) {
        try {
            return objectMapper.readValue(source, DomainEvent.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize command", e);
        }
    }

    @Override
    public String toString(DomainEvent command) {
        try {
            return objectMapper.writeValueAsString(command);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize command", e);
        }
    }

}
