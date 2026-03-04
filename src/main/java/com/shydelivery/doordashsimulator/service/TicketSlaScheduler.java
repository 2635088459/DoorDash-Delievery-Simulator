package com.shydelivery.doordashsimulator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shydelivery.doordashsimulator.entity.Ticket;
import com.shydelivery.doordashsimulator.entity.Ticket.TicketPriority;
import com.shydelivery.doordashsimulator.entity.Ticket.TicketStatus;
import com.shydelivery.doordashsimulator.entity.TicketComment;
import com.shydelivery.doordashsimulator.entity.TicketComment.CommentType;
import com.shydelivery.doordashsimulator.repository.TicketCommentRepository;
import com.shydelivery.doordashsimulator.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketSlaScheduler {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 */30 * * * ?")
    public void checkSla() {
        LocalDateTime now = LocalDateTime.now();
        List<Ticket> tickets = ticketRepository.findByStatusIn(List.of(
            TicketStatus.NEW,
            TicketStatus.IN_PROGRESS
        ));

        for (Ticket ticket : tickets) {
            long slaHours = getSlaHours(ticket.getPriority());
            LocalDateTime deadline = ticket.getCreatedAt().plusHours(slaHours);
            if (!now.isAfter(deadline)) {
                continue;
            }

            if (recentSlaAlertExists(ticket, now)) {
                continue;
            }

            long overdueMinutes = Duration.between(deadline, now).toMinutes();
            Map<String, Object> evidence = new java.util.LinkedHashMap<>();
            evidence.put("summaryType", "SLA_ALERT");
            evidence.put("slaHours", slaHours);
            evidence.put("overdueMinutes", overdueMinutes);
            evidence.put("status", ticket.getStatus().name());
            evidence.put("assignedRole", ticket.getAssignedRole());

            String message = String.format("SLA 超时提醒: 已超时 %.1f 小时", overdueMinutes / 60.0);

            TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .author("SYSTEM")
                .authorRole("SYSTEM")
                .type(CommentType.SYSTEM_NOTE)
                .content(message)
                .evidenceJson(toEvidenceJson(evidence))
                .build();

            ticketCommentRepository.save(comment);
        }
    }

    private boolean recentSlaAlertExists(Ticket ticket, LocalDateTime now) {
        TicketComment latest = ticketCommentRepository.findTop1ByTicketAndTypeOrderByCreatedAtDesc(
            ticket,
            CommentType.SYSTEM_NOTE
        );
        if (latest == null || latest.getEvidenceJson() == null) {
            return false;
        }
        try {
            Map<String, Object> evidence = objectMapper.readValue(
                latest.getEvidenceJson(),
                new TypeReference<Map<String, Object>>() {}
            );
            if (!"SLA_ALERT".equals(evidence.get("summaryType"))) {
                return false;
            }
        } catch (Exception ex) {
            return false;
        }
        return latest.getCreatedAt().isAfter(now.minusHours(4));
    }

    private long getSlaHours(TicketPriority priority) {
        if (priority == null) {
            return 8;
        }
        return switch (priority) {
            case URGENT -> 2;
            case HIGH -> 4;
            case NORMAL -> 8;
            case LOW -> 24;
        };
    }

    private String toEvidenceJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("Failed to serialize SLA evidence: {}", ex.getMessage());
            return null;
        }
    }
}
