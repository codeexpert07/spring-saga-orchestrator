package org.codeexpert.order.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LongVarcharJdbcType;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.support.DefaultStateMachineContext;

@Entity
@Table(name = "state_machines")
@Data
public class StateMachineEntity {
    
    @Id
    @Column(name = "machine_id")
    private String machineId;
    
    @Column(name = "state")
    private String state;
    
    @Lob
    @Column(name = "state_machine_context")
    @JdbcType(LongVarcharJdbcType.class)
    private byte[] stateMachineContext;
    
    @Version
    private Long version;
    
    public StateMachineEntity() {
        // Default constructor
    }
    
    public StateMachineContext<?, ?> getStateMachineContext() {
        if (stateMachineContext == null) {
            return null;
        }
        return new StateMachineContextJsonSerializer<>().deserialize(stateMachineContext);
    }
    
    public void setStateMachineContext(StateMachineContext<?, ?> context) {
        if (context != null) {
            this.stateMachineContext = new StateMachineContextJsonSerializer<>().serialize(context);
        } else {
            this.stateMachineContext = null;
        }
    }
}
