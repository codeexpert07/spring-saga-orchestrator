//package org.codeexpert.order.config;
//
//import org.codeexpert.order.model.OrderState;
//import org.codeexpert.order.model.OrderEvent;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.statemachine.StateMachineContext;
//import org.springframework.statemachine.StateMachinePersist;
//import org.springframework.statemachine.config.EnableStateMachineFactory;
//import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
//import org.springframework.statemachine.config.StateMachineFactory;
//import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
//import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
//import org.springframework.statemachine.persist.DefaultStateMachinePersister;
//import org.springframework.statemachine.persist.StateMachinePersister;
//import org.springframework.statemachine.service.DefaultStateMachineService;
//import org.springframework.statemachine.service.StateMachineService;
//import org.springframework.statemachine.persist.StateMachineRuntimePersister;
//
//import java.util.EnumSet;
//
//@Configuration
//
//public class StateMachineConfig extends EnumStateMachineConfigurerAdapter<OrderState, OrderEvent> {
//
//    @Override
//    public void configure(StateMachineStateConfigurer<OrderState, OrderEvent> states) throws Exception {
//        states
//            .withStates()
//                .initial(OrderState.PENDING)
//                .states(EnumSet.allOf(OrderState.class));
//    }
//
//    @Override
//    public void configure(org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer<OrderState, OrderEvent> config)
//            throws Exception {
//        config
//            .withPersistence()
//            .and()
//            .withConfiguration()
//                .autoStartup(true);
//    }
//
//
//}
