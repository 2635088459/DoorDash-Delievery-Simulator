package com.shydelivery.doordashsimulator.repository;

import com.shydelivery.doordashsimulator.entity.Ticket;
import com.shydelivery.doordashsimulator.entity.Ticket.TicketCategory;
import com.shydelivery.doordashsimulator.entity.Ticket.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatusOrderByCreatedAtDesc(TicketStatus status);

    List<Ticket> findByStatusIn(List<TicketStatus> statuses);

    List<Ticket> findByStatusAndUpdatedAtBefore(TicketStatus status, LocalDateTime cutoff);

    boolean existsByCategoryAndStatusInAndRestaurantId(
        TicketCategory category,
        List<TicketStatus> status,
        Long restaurantId
    );
}
