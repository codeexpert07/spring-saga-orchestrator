package org.codeexpert.order.config;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.persist.AbstractStateMachinePersister;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.support.StateMachineInterceptor;

import java.util.HashMap;
import java.util.Map;

public class JpaStateMachineRuntimePersister<S, E, T>
        extends AbstractStateMachinePersister<S, E, T>
        implements StateMachineRuntimePersister<S, E, T> {

    private final Map<T, StateMachineContext<S, E>> contextMap = new HashMap<>();

    public JpaStateMachineRuntimePersister() {
        super(null);
    }

    @Override
    public void write(StateMachineContext<S, E> context, T contextObj) {
        contextMap.put(contextObj, context);
    }

    @Override
    public StateMachineContext<S, E> read(T contextObj) {
        return contextMap.get(contextObj);
    }

    /*@Override
    public StateMachinePersist<S, E, T> getStateMachinePersist() {
        return new StateMachinePersist<>() {
            @Override
            public void write(StateMachineContext<S, E> context, T contextObj) {
                JpaStateMachineRuntimePersister.this.write(context, contextObj);
            }

            @Override
            public StateMachineContext<S, E> read(T contextObj) {
                StateMachineContext<S, E> context = JpaStateMachineRuntimePersister.this.read(contextObj);
                return context != null ? context : new DefaultStateMachineContext<>(null, null, null, null);
            }
        };
    }*/

    @Override
    public StateMachineInterceptor<S, E> getInterceptor() {
        return null;
    }
}