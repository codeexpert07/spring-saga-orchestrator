package org.codeexpert.order.config;

import org.codeexpert.order.model.OrderState;
import org.codeexpert.order.model.OrderEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.statemachine.service.DefaultStateMachineService;
import org.springframework.statemachine.service.StateMachineService;

import java.util.EnumSet;

@Configuration
public class StateMachineConfig extends EnumStateMachineConfigurerAdapter<OrderState, OrderEvent> {

    private final JpaStateMachineRepository stateMachineRepository;

    public StateMachineConfig(JpaStateMachineRepository stateMachineRepository) {
        this.stateMachineRepository = stateMachineRepository;
    }

    @Override
    public void configure(StateMachineStateConfigurer<OrderState, OrderEvent> states) throws Exception {
        states
            .withStates()
                .initial(OrderState.PENDING)
                .states(EnumSet.allOf(OrderState.class));
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<OrderState, OrderEvent> config) throws Exception {
        config
            .withPersistence()
                .runtimePersister(stateMachineRuntimePersister())
            .and()
            .withConfiguration()
                .autoStartup(true);
    }

    @Bean
    public StateMachineRuntimePersister<OrderState, OrderEvent, String> stateMachineRuntimePersister() {
        return new JpaStateMachineRuntimePersister<>();
    }

    @Bean
    public StateMachineService<OrderState, OrderEvent> stateMachineService(
            org.springframework.statemachine.config.StateMachineFactory<OrderState, OrderEvent> stateMachineFactory) {
        return new DefaultStateMachineService<>(
            stateMachineFactory,
            stateMachineRuntimePersister()
        );
    }
}
