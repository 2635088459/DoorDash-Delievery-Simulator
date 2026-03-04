package com.shydelivery.doordashsimulator.repository;

import com.shydelivery.doordashsimulator.entity.Ticket;
import com.shydelivery.doordashsimulator.entity.TicketComment;
import com.shydelivery.doordashsimulator.entity.TicketComment.CommentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

    List<TicketComment> findByTicketOrderByCreatedAtAsc(Ticket ticket);

    TicketComment findTop1ByTicketAndTypeOrderByCreatedAtDesc(Ticket ticket, CommentType type);
}
