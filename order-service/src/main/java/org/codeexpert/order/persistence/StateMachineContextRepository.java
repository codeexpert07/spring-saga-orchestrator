package org.codeexpert.order.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StateMachineContextRepository extends JpaRepository<StateMachineContextEntity, String> {
}
