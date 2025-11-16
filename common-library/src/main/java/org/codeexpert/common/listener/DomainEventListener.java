package org.codeexpert.common.listener;

import org.codeexpert.common.event.DomainEvent;

public interface DomainEventListener {
    void onEvent(DomainEvent event);
}
