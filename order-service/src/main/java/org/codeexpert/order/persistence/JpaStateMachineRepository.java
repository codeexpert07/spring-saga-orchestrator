package org.codeexpert.order.persistence;

import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Optional;

@Component
public class JpaStateMachineRepository<S, E> implements StateMachinePersist<S, E, String> {

    private final StateMachineContextRepository repository;

    public JpaStateMachineRepository(StateMachineContextRepository repository) {
        this.repository = repository;
    }

    @Override
    public void write(StateMachineContext<S, E> context, String contextObj) throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            
            oos.writeObject(context);
            oos.flush();
            
            StateMachineContextEntity entity = new StateMachineContextEntity();
            entity.setMachineId(contextObj);
            entity.setContext(bos.toByteArray());
            
            repository.save(entity);
        }
    }

    @Override
    public StateMachineContext<S, E> read(String contextObj) throws Exception {
        Optional<StateMachineContextEntity> entityOpt = repository.findById(contextObj);
        if (!entityOpt.isPresent()) {
            return null;
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(entityOpt.get().getContext());
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            
            @SuppressWarnings("unchecked")
            StateMachineContext<S, E> context = (StateMachineContext<S, E>) ois.readObject();
            return context;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Error reading state machine context", e);
        }
    }
}
