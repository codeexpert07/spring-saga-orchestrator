package org.codeexpert.order.repository;

import org.codeexpert.order.model.StateMachineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StateMachineRepository extends JpaRepository<StateMachineEntity, String> {
    // Custom query methods can be added here if needed
}
