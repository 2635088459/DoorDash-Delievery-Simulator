package com.shydelivery.doordashsimulator.repository;

import com.shydelivery.doordashsimulator.entity.Ticket;
import com.shydelivery.doordashsimulator.entity.TicketActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketActionLogRepository extends JpaRepository<TicketActionLog, Long> {

    List<TicketActionLog> findByTicketOrderByCreatedAtDesc(Ticket ticket);

    List<TicketActionLog> findAllByOrderByCreatedAtDesc();
}
