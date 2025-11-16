package org.codeexpert.order.config;

import org.codeexpert.order.model.OrderEvent;
import org.codeexpert.order.model.OrderState;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.statemachine.data.jpa.JpaPersistingStateMachineInterceptor;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;

@Configuration
@EntityScan(basePackages = {
    "org.springframework.statemachine.data.jpa"
})
@EnableJpaRepositories(basePackages = {
    "org.springframework.statemachine.data.jpa"
})
public class StateMachineJpaConfig {

    @Bean
    public StateMachineRuntimePersister<OrderState, OrderEvent, String> stateMachineRuntimePersister(
            JpaStateMachineRepository jpaStateMachineRepository) {
        return new JpaPersistingStateMachineInterceptor<>(jpaStateMachineRepository);
    }
}
