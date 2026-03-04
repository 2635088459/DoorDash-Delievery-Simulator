package com.shydelivery.doordashsimulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shydelivery.doordashsimulator.entity.Ticket;
import com.shydelivery.doordashsimulator.entity.Ticket.TicketStatus;
import com.shydelivery.doordashsimulator.entity.TicketComment;
import com.shydelivery.doordashsimulator.entity.TicketComment.CommentType;
import com.shydelivery.doordashsimulator.repository.TicketCommentRepository;
import com.shydelivery.doordashsimulator.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketAutoCloseScheduler {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.ticket.auto-close-hours:24}")
    private long autoCloseHours;

    @Scheduled(cron = "0 */60 * * * ?")
    public void autoCloseResolvedTickets() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(autoCloseHours);
        List<Ticket> tickets = ticketRepository.findByStatusAndUpdatedAtBefore(TicketStatus.RESOLVED, cutoff);

        for (Ticket ticket : tickets) {
            ticket.setStatus(TicketStatus.CLOSED);
            ticketRepository.save(ticket);

            Map<String, Object> evidence = new java.util.LinkedHashMap<>();
            evidence.put("summaryType", "AUTO_CLOSE");
            evidence.put("reason", String.format("RESOLVED 超过 %d 小时自动关闭", autoCloseHours));
            evidence.put("statusBefore", TicketStatus.RESOLVED.name());
            evidence.put("statusAfter", TicketStatus.CLOSED.name());

            TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .author("SYSTEM")
                .authorRole("SYSTEM")
                .type(CommentType.SYSTEM_NOTE)
                .content(String.format("系统自动关闭工单：RESOLVED 超过 %d 小时", autoCloseHours))
                .evidenceJson(toEvidenceJson(evidence))
                .build();

            ticketCommentRepository.save(comment);
        }
    }

    private String toEvidenceJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("Failed to serialize auto-close evidence: {}", ex.getMessage());
            return null;
        }
    }
}
