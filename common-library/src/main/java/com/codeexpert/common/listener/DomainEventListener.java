package com.codeexpert.common.listener;

import com.codeexpert.common.event.DomainEvent;

public interface DomainEventListener {
    void onEvent(DomainEvent event);
}
