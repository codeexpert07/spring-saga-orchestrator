package org.codeexpert.order.config;

import org.codeexpert.order.model.StateMachineEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.data.StateMachineRepository;
import org.springframework.statemachine.service.StateMachineSerialisationService;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.spring.StateMachineExecutorService;
import org.springframework.statemachine.support.DefaultStateMachineContext;

@Configuration
public class StateMachineJpaConfig {

    @Bean
    public StateMachineRepository<StateMachineEntity> stateMachineRepository(
            JpaRepository<StateMachineEntity, String> jpaRepository) {
        return new StateMachineRepository<>() {
            @Override
            public StateMachineEntity findById(String machineId) {
                return jpaRepository.findById(machineId).orElse(null);
            }

            @Override
            public void save(StateMachineEntity stateMachine) {
                jpaRepository.save(stateMachine);
            }

            @Override
            public void delete(StateMachineEntity stateMachine) {
                jpaRepository.delete(stateMachine);
            }
        };
    }

    @Bean
    public StateMachineService<StateMachineEntity> stateMachineService(
            StateMachineRepository<StateMachineEntity> stateMachineRepository,
            StateMachineSerialisationService<StateMachineEntity> stateMachineSerialisationService,
            StateMachineExecutorService stateMachineExecutorService) {
        return new StateMachineService<>(
            stateMachineRepository,
            stateMachineSerialisationService,
            stateMachineExecutorService,
            (id) -> new DefaultStateMachineContext<>(null, null, null, null)
        );
    }
}
