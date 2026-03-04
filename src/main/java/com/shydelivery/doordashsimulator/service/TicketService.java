package com.shydelivery.doordashsimulator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shydelivery.doordashsimulator.dto.request.AddTicketCommentRequest;
import com.shydelivery.doordashsimulator.dto.request.CreateTicketRequest;
import com.shydelivery.doordashsimulator.dto.request.ExecuteTicketActionRequest;
import com.shydelivery.doordashsimulator.dto.request.UpdateTicketActionResultRequest;
import com.shydelivery.doordashsimulator.dto.request.UpdateTicketStatusRequest;
import com.shydelivery.doordashsimulator.dto.response.TicketActionLogDTO;
import com.shydelivery.doordashsimulator.dto.response.TicketCommentDTO;
import com.shydelivery.doordashsimulator.dto.response.TicketDTO;
import com.shydelivery.doordashsimulator.dto.response.TicketSampleOrderDTO;
import com.shydelivery.doordashsimulator.dto.response.TicketSummaryDTO;
import com.shydelivery.doordashsimulator.entity.Ticket;
import com.shydelivery.doordashsimulator.entity.Ticket.TicketCategory;
import com.shydelivery.doordashsimulator.entity.Ticket.TicketPriority;
import com.shydelivery.doordashsimulator.entity.Ticket.TicketSource;
import com.shydelivery.doordashsimulator.entity.Ticket.TicketStatus;
import com.shydelivery.doordashsimulator.entity.TicketComment;
import com.shydelivery.doordashsimulator.entity.TicketComment.CommentType;
import com.shydelivery.doordashsimulator.entity.TicketActionLog;
import com.shydelivery.doordashsimulator.entity.TicketActionLog.ActionStatus;
import com.shydelivery.doordashsimulator.entity.Order;
import com.shydelivery.doordashsimulator.entity.Order.OrderStatus;
import com.shydelivery.doordashsimulator.entity.Review;
import com.shydelivery.doordashsimulator.exception.ResourceNotFoundException;
import com.shydelivery.doordashsimulator.repository.TicketCommentRepository;
import com.shydelivery.doordashsimulator.repository.TicketRepository;
import com.shydelivery.doordashsimulator.repository.OrderRepository;
import com.shydelivery.doordashsimulator.repository.ReviewRepository;
import com.shydelivery.doordashsimulator.repository.TicketActionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final TicketActionLogRepository ticketActionLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TicketDTO createTicket(CreateTicketRequest request, String createdBy) {
        Ticket ticket = Ticket.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .category(request.getCategory())
            .priority(request.getPriority() == null ? TicketPriority.NORMAL : request.getPriority())
            .source(request.getSource() == null ? TicketSource.MANUAL : request.getSource())
            .restaurantId(request.getRestaurantId())
            .orderId(request.getOrderId())
            .driverId(request.getDriverId())
            .assignedTo(request.getAssignedTo())
            .assignedRole(request.getAssignedRole())
            .createdBy(createdBy)
            .evidenceJson(request.getEvidenceJson())
            .status(TicketStatus.NEW)
            .build();

        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket created: id={}, category={}", saved.getId(), saved.getCategory());

        addAgentSuggestion(saved);
        return convertToDTO(saved, true);
    }

    @Transactional
    public TicketDTO createSystemTicket(
        String title,
        String description,
        TicketCategory category,
        TicketPriority priority,
        Long restaurantId,
        String evidenceJson
    ) {
        Ticket ticket = Ticket.builder()
            .title(title)
            .description(description)
            .category(category)
            .priority(priority == null ? TicketPriority.NORMAL : priority)
            .source(TicketSource.SYSTEM)
            .restaurantId(restaurantId)
            .assignedRole(defaultAssignedRole(category))
            .createdBy("SYSTEM")
            .evidenceJson(evidenceJson)
            .status(TicketStatus.NEW)
            .build();

        Ticket saved = ticketRepository.save(ticket);
        addEvidenceSummary(saved);
        addAgentSuggestion(saved);
        return convertToDTO(saved, true);
    }

    @Transactional(readOnly = true)
    public List<TicketDTO> getAllTickets() {
        return ticketRepository.findAll().stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .map(ticket -> convertToDTO(ticket, false))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TicketDTO getTicketById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found, ID: " + id));
        return convertToDTO(ticket, true);
    }

    @Transactional
    public TicketDTO updateStatus(Long id, UpdateTicketStatusRequest request, String operator) {
        Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found, ID: " + id));
        TicketStatus previousStatus = ticket.getStatus();
        String previousAssignedTo = ticket.getAssignedTo();
        String previousAssignedRole = ticket.getAssignedRole();

        ticket.setStatus(request.getStatus());
        if (request.getAssignedTo() != null && !request.getAssignedTo().isBlank()) {
            ticket.setAssignedTo(request.getAssignedTo());
        }
        if (request.getAssignedRole() != null && !request.getAssignedRole().isBlank()) {
            ticket.setAssignedRole(request.getAssignedRole());
        }

        Ticket saved = ticketRepository.save(ticket);

        StringBuilder message = new StringBuilder();
        message.append(operator).append(" updated ticket status to ").append(saved.getStatus());
        if (saved.getAssignedTo() != null && !saved.getAssignedTo().isBlank()) {
            message.append(", assigned to ").append(saved.getAssignedTo());
        }
        if (saved.getAssignedRole() != null && !saved.getAssignedRole().isBlank()) {
            message.append(" (role ").append(saved.getAssignedRole()).append(")");
        }

        Map<String, Object> evidence = new java.util.HashMap<>();
        evidence.put("operator", operator);
        evidence.put("status", saved.getStatus().name());
        evidence.put("assignedTo", saved.getAssignedTo());
        evidence.put("assignedRole", saved.getAssignedRole());
        evidence.put("previousStatus", previousStatus == null ? null : previousStatus.name());
        evidence.put("previousAssignedTo", previousAssignedTo);
        evidence.put("previousAssignedRole", previousAssignedRole);

        addSystemComment(saved, message.toString(), toEvidenceJson(evidence));

        return convertToDTO(saved, true);
    }

    @Transactional
    public TicketCommentDTO addComment(Long ticketId, AddTicketCommentRequest request, String author) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found, ID: " + ticketId));

        TicketComment comment = TicketComment.builder()
            .ticket(ticket)
            .author(author)
            .authorRole("USER")
            .type(request.getType() == null ? CommentType.USER_NOTE : request.getType())
            .content(request.getContent())
            .evidenceJson(request.getEvidenceJson())
            .build();

        TicketComment saved = ticketCommentRepository.save(comment);
        return convertToDTO(saved);
    }

    @Transactional
    public TicketDTO executeAction(Long ticketId, ExecuteTicketActionRequest request, String operator) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found, ID: " + ticketId));

        TicketStatus beforeStatus = ticket.getStatus();
        if (request.isMarkResolved()) {
            ticket.setStatus(TicketStatus.RESOLVED);
        } else if (ticket.getStatus() == TicketStatus.NEW) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }

        Ticket saved = ticketRepository.save(ticket);

        TicketActionLog actionLog = TicketActionLog.builder()
            .ticket(saved)
            .actionType(request.getActionType())
            .status(ActionStatus.PENDING)
            .operator(operator)
            .note(request.getNote())
            .resultMessage("Awaiting result write-back")
            .build();

        TicketActionLog savedLog = ticketActionLogRepository.save(actionLog);

        Map<String, Object> evidence = new java.util.LinkedHashMap<>();
        evidence.put("summaryType", "ACTION_EXECUTION");
        evidence.put("actionType", request.getActionType());
        evidence.put("actionLogId", savedLog.getId());
        evidence.put("operator", operator);
        evidence.put("note", request.getNote());
        evidence.put("statusBefore", beforeStatus.name());
        evidence.put("statusAfter", saved.getStatus().name());

        StringBuilder message = new StringBuilder();
        message.append("Agent executed action: ").append(request.getActionType());
        if (request.getNote() != null && !request.getNote().isBlank()) {
            message.append(" · ").append(request.getNote());
        }
        message.append(" (status: ").append(beforeStatus).append(" → ").append(saved.getStatus()).append(")");

        addSystemComment(saved, message.toString(), toEvidenceJson(evidence));

        return convertToDTO(saved, true);
    }

    @Transactional(readOnly = true)
    public List<TicketActionLogDTO> getActionLogs(Long ticketId) {
        List<TicketActionLog> logs;
        if (ticketId == null) {
            logs = ticketActionLogRepository.findAllByOrderByCreatedAtDesc();
        } else {
            Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found, ID: " + ticketId));
            logs = ticketActionLogRepository.findByTicketOrderByCreatedAtDesc(ticket);
        }
        return logs.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public TicketActionLogDTO updateActionResult(
        Long ticketId,
        Long actionId,
        UpdateTicketActionResultRequest request,
        String operator
    ) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found, ID: " + ticketId));
        TicketStatus beforeStatus = ticket.getStatus();
        TicketActionLog logEntry = ticketActionLogRepository.findById(actionId)
            .orElseThrow(() -> new ResourceNotFoundException("Action log not found, ID: " + actionId));

        if (!logEntry.getTicket().getId().equals(ticket.getId())) {
            throw new ResourceNotFoundException("Action log does not belong to this ticket");
        }

        logEntry.setStatus(request.getStatus());
        logEntry.setResultMessage(request.getResultMessage());
        logEntry.setOperator(operator);

        TicketActionLog savedLog = ticketActionLogRepository.save(logEntry);

        boolean autoResolved = false;
        if (savedLog.getStatus() == ActionStatus.SUCCESS
            && ticket.getStatus() != TicketStatus.RESOLVED
            && ticket.getStatus() != TicketStatus.CLOSED) {
            ticket.setStatus(TicketStatus.RESOLVED);
            ticketRepository.save(ticket);
            autoResolved = true;
        }

        Map<String, Object> evidence = new java.util.LinkedHashMap<>();
        evidence.put("summaryType", "ACTION_RESULT");
        evidence.put("actionType", savedLog.getActionType());
        evidence.put("actionLogId", savedLog.getId());
        evidence.put("resultStatus", savedLog.getStatus().name());
        evidence.put("resultMessage", savedLog.getResultMessage());
        evidence.put("operator", operator);
    evidence.put("autoResolved", autoResolved);
    evidence.put("statusBefore", beforeStatus == null ? null : beforeStatus.name());
    evidence.put("statusAfter", ticket.getStatus() == null ? null : ticket.getStatus().name());

        StringBuilder message = new StringBuilder();
        message.append("Action result write-back: ").append(savedLog.getActionType())
            .append(" · ").append(savedLog.getStatus());
        if (savedLog.getResultMessage() != null && !savedLog.getResultMessage().isBlank()) {
            message.append(" · ").append(savedLog.getResultMessage());
        }
        if (autoResolved) {
            message.append(" · Ticket auto-marked as resolved");
        }

        addSystemComment(ticket, message.toString(), toEvidenceJson(evidence));

        return convertToDTO(savedLog);
    }

    @Transactional(readOnly = true)
    public List<TicketSampleOrderDTO> getSampleOrders(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found, ID: " + ticketId));

        if (ticket.getRestaurantId() == null) {
            return Collections.emptyList();
        }

        List<Order> orders = switch (ticket.getCategory()) {
            case RESTAURANT_CANCEL_SPIKE ->
                orderRepository.findTop10ByRestaurantIdAndStatusOrderByCreatedAtDesc(
                    ticket.getRestaurantId(), OrderStatus.CANCELLED);
            case DELIVERY_DELAY_SPIKE, DELIVERY_TIMEOUT_SPIKE ->
                orderRepository.findTop10ByRestaurantIdAndStatusOrderByCreatedAtDesc(
                    ticket.getRestaurantId(), OrderStatus.DELIVERED);
            case PAYMENT_REFUND_SPIKE ->
                orderRepository.findTop10ByRestaurantIdAndPaymentStatusOrderByCreatedAtDesc(
                    ticket.getRestaurantId(), Order.PaymentStatus.REFUNDED);
            default -> orderRepository.findTop10ByRestaurantIdOrderByCreatedAtDesc(ticket.getRestaurantId());
        };

        return orders.stream()
            .map(this::convertToSampleDTO)
            .collect(Collectors.toList());
    }

    private void addAgentSuggestion(Ticket ticket) {
        String suggestion = buildAgentSuggestion(ticket);
        String toolNote = "\n\nTools used: metrics.summary, orders.sample, reviews.rating";
        Map<String, Object> evidence = buildAgentEvidence(ticket);
        TicketComment comment = TicketComment.builder()
            .ticket(ticket)
            .author("AGENT")
            .authorRole("ASSISTANT")
            .type(CommentType.AGENT_NOTE)
            .content(suggestion + toolNote)
            .evidenceJson(toEvidenceJson(evidence))
            .build();

        ticketCommentRepository.save(comment);
    }

    private void addEvidenceSummary(Ticket ticket) {
        Map<String, Object> evidence = buildEvidenceSummary(ticket);
        String message = evidence.get("summaryText") instanceof String
            ? (String) evidence.get("summaryText")
            : "Evidence summary generated";
        addSystemComment(ticket, message, toEvidenceJson(evidence));
    }

    private Map<String, Object> buildEvidenceSummary(Ticket ticket) {
        Map<String, Object> summary = new java.util.LinkedHashMap<>();
        Map<String, Object> anomalyEvidence = null;
        if (ticket.getEvidenceJson() != null) {
            try {
                anomalyEvidence = objectMapper.readValue(ticket.getEvidenceJson(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ex) {
                log.warn("Failed to parse ticket evidence json: {}", ex.getMessage());
            }
        }

        summary.put("summaryType", "EVIDENCE_SNAPSHOT");
        summary.put("category", ticket.getCategory());
        summary.put("assignedRole", ticket.getAssignedRole());
        summary.put("anomalyEvidence", anomalyEvidence);
        summary.put("reasonTags", buildReasonTags(ticket, anomalyEvidence));
        summary.put("ratingSummary", buildRestaurantRatingSummary(ticket.getRestaurantId()));
        summary.put("reviewHighlights", buildReviewHighlights(ticket.getRestaurantId()));
        summary.put("summaryText", buildEvidenceSummaryText(ticket, anomalyEvidence));
        return summary;
    }

    private Map<String, Object> buildAgentEvidence(Ticket ticket) {
        Map<String, Object> evidence = new java.util.LinkedHashMap<>();
        Map<String, Object> anomalyEvidence = null;
        if (ticket.getEvidenceJson() != null) {
            try {
                anomalyEvidence = objectMapper.readValue(ticket.getEvidenceJson(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ex) {
                log.warn("Failed to parse ticket evidence json: {}", ex.getMessage());
            }
        }

        evidence.put("ticketId", ticket.getId());
        evidence.put("category", ticket.getCategory());
        evidence.put("assignedRole", ticket.getAssignedRole());
    evidence.put("anomalyEvidence", anomalyEvidence);
    evidence.put("reasonTags", buildReasonTags(ticket, anomalyEvidence));
    evidence.put("hypotheses", buildHypotheses(ticket, anomalyEvidence));
    evidence.put("checklist", buildChecklist(ticket, anomalyEvidence));
    evidence.put("actionPlan", buildActionPlan(ticket, anomalyEvidence));

        if (ticket.getRestaurantId() == null) {
            return evidence;
        }

        List<Order> orders = switch (ticket.getCategory()) {
            case RESTAURANT_CANCEL_SPIKE ->
                orderRepository.findTop10ByRestaurantIdAndStatusOrderByCreatedAtDesc(
                    ticket.getRestaurantId(), OrderStatus.CANCELLED);
            case DELIVERY_DELAY_SPIKE, DELIVERY_TIMEOUT_SPIKE ->
                orderRepository.findTop10ByRestaurantIdAndStatusOrderByCreatedAtDesc(
                    ticket.getRestaurantId(), OrderStatus.DELIVERED);
            case PAYMENT_REFUND_SPIKE ->
                orderRepository.findTop10ByRestaurantIdAndPaymentStatusOrderByCreatedAtDesc(
                    ticket.getRestaurantId(), Order.PaymentStatus.REFUNDED);
            default -> orderRepository.findTop10ByRestaurantIdOrderByCreatedAtDesc(ticket.getRestaurantId());
        };

        List<Map<String, Object>> samples = orders.stream()
            .limit(5)
            .map(order -> {
                Map<String, Object> item = new java.util.LinkedHashMap<>();
                item.put("orderId", order.getId());
                item.put("orderNumber", order.getOrderNumber());
                item.put("status", order.getStatus());
                item.put("paymentStatus", order.getPaymentStatus());
                item.put("totalAmount", order.getTotalAmount());
                item.put("createdAt", order.getCreatedAt());
                item.put("actualDelivery", order.getActualDelivery());
                item.put("estimatedDelivery", order.getEstimatedDelivery());
                item.put("anomalyReason", buildOrderReason(ticket, order));
                return item;
            })
            .collect(Collectors.toList());

        evidence.put("sampleOrders", samples);
        evidence.put("sampleCount", samples.size());
        evidence.put("reviews", buildRestaurantReviews(ticket.getRestaurantId()));
        evidence.put("ratingSummary", buildRestaurantRatingSummary(ticket.getRestaurantId()));
        return evidence;
    }

    private List<String> buildReasonTags(Ticket ticket, Map<String, Object> anomalyEvidence) {
        List<String> tags = new java.util.ArrayList<>();
        if (ticket.getCategory() == TicketCategory.RESTAURANT_CANCEL_SPIKE) {
            tags.add("Cancel rate spike");
        }
        if (ticket.getCategory() == TicketCategory.DELIVERY_DELAY_SPIKE) {
            tags.add("Delivery delays elevated");
        }
        if (ticket.getCategory() == TicketCategory.DELIVERY_TIMEOUT_SPIKE) {
            tags.add("Timeout orders spiking");
        }
        if (ticket.getCategory() == TicketCategory.PAYMENT_REFUND_SPIKE) {
            tags.add("Refund rate anomaly");
        }
        if (anomalyEvidence != null) {
            Object avgDelay = anomalyEvidence.get("avgDelayMinutes");
            if (avgDelay instanceof Number && ((Number) avgDelay).doubleValue() >= 20) {
                tags.add("Average delay > 20 minutes");
            }
        }
        return tags;
    }

    private String buildOrderReason(Ticket ticket, Order order) {
        return switch (ticket.getCategory()) {
            case RESTAURANT_CANCEL_SPIKE -> order.getStatus() == OrderStatus.CANCELLED ? "Order cancelled" : "Related anomaly";
            case DELIVERY_DELAY_SPIKE -> "Delivered later than ETA";
            case DELIVERY_TIMEOUT_SPIKE -> "Over 30 minutes late";
            case PAYMENT_REFUND_SPIKE -> order.getPaymentStatus() == Order.PaymentStatus.REFUNDED ? "Payment refunded" : "Refund related";
            default -> "Anomalous sample";
        };
    }

    private List<Map<String, Object>> buildRestaurantReviews(Long restaurantId) {
        if (restaurantId == null) {
            return Collections.emptyList();
        }
        return reviewRepository.findTop5ByRestaurantIdOrderByCreatedAtDesc(restaurantId)
            .stream()
            .map(review -> {
                Map<String, Object> item = new java.util.LinkedHashMap<>();
                item.put("rating", review.getOverallRating());
                item.put("foodRating", review.getFoodRating());
                item.put("deliveryRating", review.getDeliveryRating());
                item.put("comment", review.getComment());
                item.put("createdAt", review.getCreatedAt());
                item.put("isPositive", review.isPositive());
                return item;
            })
            .collect(Collectors.toList());
    }

    private Map<String, Object> buildRestaurantRatingSummary(Long restaurantId) {
        if (restaurantId == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> summary = new java.util.LinkedHashMap<>();
        summary.put("averageRating", reviewRepository.calculateAverageRatingForRestaurant(restaurantId));
        summary.put("averageFoodRating", reviewRepository.calculateAverageFoodRatingForRestaurant(restaurantId));
        summary.put("averageDeliveryRating", reviewRepository.calculateAverageDeliveryRatingForRestaurant(restaurantId));
        summary.put("totalReviews", reviewRepository.countReviewsByRestaurant(restaurantId));
        return summary;
    }

    private List<String> buildReviewHighlights(Long restaurantId) {
        if (restaurantId == null) {
            return Collections.emptyList();
        }
        return reviewRepository.findTop5ByRestaurantIdOrderByCreatedAtDesc(restaurantId)
            .stream()
            .map(Review::getComment)
            .filter(comment -> comment != null && !comment.isBlank())
            .limit(2)
            .collect(Collectors.toList());
    }

    private String buildEvidenceSummaryText(Ticket ticket, Map<String, Object> anomalyEvidence) {
        StringBuilder builder = new StringBuilder();
        builder.append("Evidence summary: ");
        if (ticket.getCategory() != null) {
            builder.append(ticket.getCategory()).append(" · ");
        }
        if (anomalyEvidence != null) {
            Object cancelRate = anomalyEvidence.get("cancelRate");
            Object delayRate = anomalyEvidence.get("delayRate");
            Object timeoutRate = anomalyEvidence.get("timeoutRate");
            Object refundRate = anomalyEvidence.get("refundRate");
            if (cancelRate instanceof Number) {
                builder.append("cancel rate ")
                    .append(String.format("%.1f%%", ((Number) cancelRate).doubleValue() * 100)).append(" ");
            }
            if (delayRate instanceof Number) {
                builder.append("delay rate ")
                    .append(String.format("%.1f%%", ((Number) delayRate).doubleValue() * 100)).append(" ");
            }
            if (timeoutRate instanceof Number) {
                builder.append("timeout rate ")
                    .append(String.format("%.1f%%", ((Number) timeoutRate).doubleValue() * 100)).append(" ");
            }
            if (refundRate instanceof Number) {
                builder.append("refund rate ")
                    .append(String.format("%.1f%%", ((Number) refundRate).doubleValue() * 100)).append(" ");
            }
        }
        Map<String, Object> ratingSummary = buildRestaurantRatingSummary(ticket.getRestaurantId());
        Object avgRating = ratingSummary.get("averageRating");
        if (avgRating instanceof Number) {
            builder.append("rating ")
                .append(String.format("%.2f", ((Number) avgRating).doubleValue()));
        }
        return builder.toString().trim();
    }

    private void addSystemComment(Ticket ticket, String message, String evidenceJson) {
        TicketComment comment = TicketComment.builder()
            .ticket(ticket)
            .author("SYSTEM")
            .authorRole("SYSTEM")
            .type(CommentType.SYSTEM_NOTE)
            .content(message)
            .evidenceJson(evidenceJson)
            .build();

        ticketCommentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public TicketSummaryDTO generateSummary(Long id) {
        Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found, ID: " + id));

        Map<String, Object> evidence = null;
        if (ticket.getEvidenceJson() != null) {
            try {
                evidence = objectMapper.readValue(ticket.getEvidenceJson(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ex) {
                log.warn("Failed to parse evidence json: {}", ex.getMessage());
            }
        }

        List<TicketComment> comments = ticketCommentRepository.findByTicketOrderByCreatedAtAsc(ticket);
        String latestSystemNote = comments.stream()
            .filter(item -> item.getType() == CommentType.SYSTEM_NOTE)
            .reduce((first, second) -> second)
            .map(TicketComment::getContent)
            .orElse(null);

        StringBuilder summary = new StringBuilder();
        summary.append("[Ticket Summary]\n");
        summary.append("Title: ").append(ticket.getTitle()).append("\n");
        summary.append("Status: ").append(ticket.getStatus()).append("\n");
        summary.append("Priority: ").append(ticket.getPriority()).append("\n");
        summary.append("Created by: ").append(ticket.getCreatedBy()).append("\n");
        if (ticket.getAssignedRole() != null) {
            summary.append("Assigned role: ").append(ticket.getAssignedRole()).append("\n");
        }
        if (ticket.getAssignedTo() != null) {
            summary.append("Assigned to: ").append(ticket.getAssignedTo()).append("\n");
        }
        if (ticket.getRestaurantId() != null) {
            summary.append("Restaurant ID: ").append(ticket.getRestaurantId()).append("\n");
        }
        if (ticket.getOrderId() != null) {
            summary.append("Order ID: ").append(ticket.getOrderId()).append("\n");
        }
        if (ticket.getDriverId() != null) {
            summary.append("Driver ID: ").append(ticket.getDriverId()).append("\n");
        }
        if (evidence != null) {
            Object cancelRate = evidence.get("cancelRate");
            Object totalOrders = evidence.get("totalOrders");
            Object cancelledOrders = evidence.get("cancelledOrders");
            if (cancelRate != null) {
                summary.append("24h cancel rate: ").append(String.format("%.1f%%", ((Number) cancelRate).doubleValue() * 100)).append("\n");
            }
            if (totalOrders != null && cancelledOrders != null) {
                summary.append("Cancelled/total: ").append(cancelledOrders).append("/").append(totalOrders).append("\n");
            }
        }
        if (latestSystemNote != null) {
            summary.append("Latest status update: ").append(latestSystemNote).append("\n");
        }
        summary.append("Recommended action: ").append("Review sample orders and the timeline to confirm whether escalation or notifications are needed.");

        return TicketSummaryDTO.builder()
            .ticketId(ticket.getId())
            .summary(summary.toString())
            .generatedAt(java.time.LocalDateTime.now())
            .build();
    }

    private String buildAgentSuggestion(Ticket ticket) {
        Map<String, Object> anomalyEvidence = null;
        if (ticket.getEvidenceJson() != null) {
            try {
                anomalyEvidence = objectMapper.readValue(ticket.getEvidenceJson(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ex) {
                log.warn("Failed to parse ticket evidence json: {}", ex.getMessage());
            }
        }

        List<String> hypotheses = buildHypotheses(ticket, anomalyEvidence);
        List<String> checklist = buildChecklist(ticket, anomalyEvidence);
        List<String> actionPlan = buildActionPlan(ticket, anomalyEvidence);
        Map<String, Object> ratingSummary = buildRestaurantRatingSummary(ticket.getRestaurantId());
        List<String> reviewHighlights = buildReviewHighlights(ticket.getRestaurantId());

        StringBuilder builder = new StringBuilder();
        builder.append("[Agent Recommendations]\n");
        builder.append("Root cause hypotheses:\n");
        for (int i = 0; i < hypotheses.size(); i++) {
            builder.append(i + 1).append(") ").append(hypotheses.get(i)).append("\n");
        }
        builder.append("\nInvestigation checklist:\n");
        for (int i = 0; i < checklist.size(); i++) {
            builder.append(i + 1).append(") ").append(checklist.get(i)).append("\n");
        }
        builder.append("\nRecommended actions:\n");
        for (int i = 0; i < actionPlan.size(); i++) {
            builder.append(i + 1).append(") ").append(actionPlan.get(i)).append("\n");
        }

        if (!ratingSummary.isEmpty()) {
            builder.append("\nRating summary: average rating ")
                .append(ratingSummary.getOrDefault("averageRating", "--"))
                .append(", total reviews ")
                .append(ratingSummary.getOrDefault("totalReviews", 0)).append("\n");
        }
        if (!reviewHighlights.isEmpty()) {
            builder.append("Recent review highlights:\n");
            reviewHighlights.forEach(comment -> builder.append("- ").append(comment).append("\n"));
        }
        return builder.toString().trim();
    }

    private List<String> buildHypotheses(Ticket ticket, Map<String, Object> anomalyEvidence) {
        List<String> hypotheses = new java.util.ArrayList<>();
        switch (ticket.getCategory()) {
            case RESTAURANT_CANCEL_SPIKE -> {
                hypotheses.add("Prep or inventory issues are driving a cancellation spike.");
                hypotheses.add("Recent menu/price changes are triggering cancellations.");
            }
            case DELIVERY_DELAY_SPIKE -> {
                hypotheses.add("Driver supply shortages or route congestion are increasing delays.");
                hypotheses.add("Longer kitchen prep times are increasing overall delays.");
            }
            case DELIVERY_TIMEOUT_SPIKE -> {
                hypotheses.add("Peak-time prep/pickup bottlenecks are causing timeout spikes.");
                hypotheses.add("Delivery radius is too large, raising timeout rates.");
            }
            case PAYMENT_REFUND_SPIKE -> {
                hypotheses.add("Payment channel issues are triggering bulk refunds.");
                hypotheses.add("Fulfillment issues are increasing refund requests.");
            }
            case PAYMENT_ISSUE -> {
                hypotheses.add("Payment gateway errors are causing payment failures.");
            }
            case DRIVER_ISSUE -> {
                hypotheses.add("Driver status or acceptance failures are causing fulfillment issues.");
            }
            default -> hypotheses.add("Further analysis of anomaly metrics and sample orders is needed.");
        }

        if (anomalyEvidence != null) {
            Object avgDelay = anomalyEvidence.get("avgDelayMinutes");
            if (avgDelay instanceof Number && ((Number) avgDelay).doubleValue() >= 30) {
                hypotheses.add("Average delay exceeds 30 minutes; severe congestion or prep issues likely.");
            }
        }

        return hypotheses;
    }

    private List<String> buildChecklist(Ticket ticket, Map<String, Object> anomalyEvidence) {
        List<String> checklist = new java.util.ArrayList<>();
        checklist.add("Compare anomaly metrics against historical baselines.");
        checklist.add("Review sample order details and anomaly reason tags.");
        if (ticket.getCategory() == TicketCategory.PAYMENT_REFUND_SPIKE || ticket.getCategory() == TicketCategory.PAYMENT_ISSUE) {
            checklist.add("Check payment gateway logs and refund reason categories.");
        }
        if (ticket.getCategory() == TicketCategory.RESTAURANT_CANCEL_SPIKE) {
            checklist.add("Contact the restaurant to confirm prep times, inventory, and menu changes.");
        }
        if (ticket.getCategory() == TicketCategory.DELIVERY_DELAY_SPIKE || ticket.getCategory() == TicketCategory.DELIVERY_TIMEOUT_SPIKE) {
            checklist.add("Check driver supply by time window and hotspot coverage.");
        }
        if (anomalyEvidence != null && anomalyEvidence.get("trendSeriesDaily") != null) {
            checklist.add("Review 7-day trends to determine if this is a short-term fluctuation.");
        }
        return checklist;
    }

    private List<String> buildActionPlan(Ticket ticket, Map<String, Object> anomalyEvidence) {
        List<String> actions = new java.util.ArrayList<>();
        switch (ticket.getCategory()) {
            case RESTAURANT_CANCEL_SPIKE -> {
                actions.add("Temporarily reduce order intake or pause orders if needed.");
                actions.add("Have ops follow up on restaurant prep and inventory.");
            }
            case DELIVERY_DELAY_SPIKE -> {
                actions.add("Dispatch additional drivers or optimize routing.");
                actions.add("Prioritize delayed orders and notify customers.");
            }
            case DELIVERY_TIMEOUT_SPIKE -> {
                actions.add("Prioritize dispatching for timeout-prone orders.");
                actions.add("Evaluate expanding compensation or coupon strategy.");
            }
            case PAYMENT_REFUND_SPIKE, PAYMENT_ISSUE -> {
                actions.add("Notify support to investigate refund/payment-failure tickets.");
                actions.add("Escalate to engineering to investigate payment flows.");
            }
            case DRIVER_ISSUE -> actions.add("Verify driver status and pause assignments if needed.");
            default -> actions.add("Form a response plan based on evidence and sample orders.");
        }
        if (anomalyEvidence != null && anomalyEvidence.get("refundRate") instanceof Number) {
            actions.add("Use the refund rate to assess whether targeted customer outreach is needed.");
        }
        return actions;
    }

    private String defaultAssignedRole(TicketCategory category) {
        if (category == null) {
            return "OPERATIONS";
        }
        return switch (category) {
            case RESTAURANT_CANCEL_SPIKE, DELIVERY_DELAY_SPIKE, DELIVERY_TIMEOUT_SPIKE, DRIVER_ISSUE -> "OPERATIONS";
            case PAYMENT_ISSUE, PAYMENT_REFUND_SPIKE -> "SUPPORT";
            default -> "ENGINEERING";
        };
    }

    private TicketDTO convertToDTO(Ticket ticket, boolean includeComments) {
        List<TicketCommentDTO> comments = includeComments
            ? ticketCommentRepository.findByTicketOrderByCreatedAtAsc(ticket)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList())
            : Collections.emptyList();

        long slaHours = getSlaHours(ticket.getPriority());
        LocalDateTime slaDeadline = ticket.getCreatedAt() == null
            ? null
            : ticket.getCreatedAt().plusHours(slaHours);
        boolean slaOverdue = false;
        long overdueMinutes = 0L;
        if (slaDeadline != null
            && (ticket.getStatus() == TicketStatus.NEW || ticket.getStatus() == TicketStatus.IN_PROGRESS)) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(slaDeadline)) {
                slaOverdue = true;
                overdueMinutes = Duration.between(slaDeadline, now).toMinutes();
            }
        }

        return TicketDTO.builder()
            .id(ticket.getId())
            .title(ticket.getTitle())
            .description(ticket.getDescription())
            .status(ticket.getStatus())
            .priority(ticket.getPriority())
            .category(ticket.getCategory())
            .source(ticket.getSource())
            .restaurantId(ticket.getRestaurantId())
            .orderId(ticket.getOrderId())
            .driverId(ticket.getDriverId())
            .assignedTo(ticket.getAssignedTo())
            .assignedRole(ticket.getAssignedRole())
            .createdBy(ticket.getCreatedBy())
            .evidenceJson(ticket.getEvidenceJson())
            .createdAt(ticket.getCreatedAt())
            .updatedAt(ticket.getUpdatedAt())
            .slaHours(slaHours)
            .slaDeadline(slaDeadline)
            .slaOverdue(slaOverdue)
            .slaOverdueMinutes(overdueMinutes)
            .comments(comments)
            .build();
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

    private TicketCommentDTO convertToDTO(TicketComment comment) {
        return TicketCommentDTO.builder()
            .id(comment.getId())
            .ticketId(comment.getTicket().getId())
            .author(comment.getAuthor())
            .authorRole(comment.getAuthorRole())
            .type(comment.getType())
            .content(comment.getContent())
            .evidenceJson(comment.getEvidenceJson())
            .createdAt(comment.getCreatedAt())
            .build();
    }

    private TicketActionLogDTO convertToDTO(TicketActionLog logEntry) {
        return TicketActionLogDTO.builder()
            .id(logEntry.getId())
            .ticketId(logEntry.getTicket().getId())
            .actionType(logEntry.getActionType())
            .status(logEntry.getStatus())
            .operator(logEntry.getOperator())
            .note(logEntry.getNote())
            .resultMessage(logEntry.getResultMessage())
            .createdAt(logEntry.getCreatedAt())
            .updatedAt(logEntry.getUpdatedAt())
            .build();
    }

    private TicketSampleOrderDTO convertToSampleDTO(Order order) {
        return TicketSampleOrderDTO.builder()
            .orderId(order.getId())
            .orderNumber(order.getOrderNumber())
            .status(order.getStatus())
            .totalAmount(order.getTotalAmount())
            .deliveryFee(order.getDeliveryFee())
            .tipAmount(order.getTipAmount())
            .customerName(order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName())
            .createdAt(order.getCreatedAt())
            .actualDelivery(order.getActualDelivery())
            .build();
    }

    public String toEvidenceJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to serialize evidence json: {}", e.getMessage());
            return null;
        }
    }
}
