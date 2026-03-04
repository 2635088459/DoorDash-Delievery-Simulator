package com.shydelivery.doordashsimulator.service;

import com.shydelivery.doordashsimulator.entity.Order;
import com.shydelivery.doordashsimulator.entity.Order.OrderStatus;
import com.shydelivery.doordashsimulator.entity.Restaurant;
import com.shydelivery.doordashsimulator.entity.Ticket.TicketCategory;
import com.shydelivery.doordashsimulator.entity.Ticket.TicketPriority;
import com.shydelivery.doordashsimulator.entity.Ticket.TicketStatus;
import com.shydelivery.doordashsimulator.repository.OrderRepository;
import com.shydelivery.doordashsimulator.repository.RestaurantRepository;
import com.shydelivery.doordashsimulator.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketAnomalyScheduler {

    private static final int MIN_SAMPLE_SIZE = 5;
    private static final double CANCEL_RATE_THRESHOLD = 0.3;
    private static final double DELAY_RATE_THRESHOLD = 0.35;
    private static final double TIMEOUT_RATE_THRESHOLD = 0.15;
    private static final double REFUND_RATE_THRESHOLD = 0.08;

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final TicketRepository ticketRepository;
    private final TicketService ticketService;

    @Scheduled(cron = "0 */10 * * * ?")
    public void detectOrderAnomalies() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusHours(24);
        LocalDateTime prevStart = now.minusHours(48);
        LocalDateTime prevEnd = now.minusHours(24);
        List<Restaurant> restaurants = restaurantRepository.findAll();

        for (Restaurant restaurant : restaurants) {
            Long total = orderRepository.countRecentOrdersByRestaurant(restaurant.getId(), since);
            if (total == null || total < MIN_SAMPLE_SIZE) {
                continue;
            }

            Long delivered = orderRepository.countRecentOrdersByRestaurantAndStatus(
                restaurant.getId(),
                OrderStatus.DELIVERED,
                since
            );
            if (delivered == null) {
                delivered = 0L;
            }

            Long cancelled = orderRepository.countRecentOrdersByRestaurantAndStatus(
                restaurant.getId(),
                OrderStatus.CANCELLED,
                since
            );
            double cancelRate = cancelled == null ? 0 : cancelled.doubleValue() / total.doubleValue();

            Long prevTotal = orderRepository.countOrdersByRestaurantBetween(
                restaurant.getId(),
                prevStart,
                prevEnd
            );
            Long prevCancelled = orderRepository.countOrdersByRestaurantAndStatusBetween(
                restaurant.getId(),
                OrderStatus.CANCELLED,
                prevStart,
                prevEnd
            );
            double prevCancelRate = (prevTotal == null || prevTotal == 0)
                ? 0
                : (prevCancelled == null ? 0 : prevCancelled.doubleValue()) / prevTotal.doubleValue();

            if (cancelRate >= CANCEL_RATE_THRESHOLD) {
                boolean exists = ticketRepository.existsByCategoryAndStatusInAndRestaurantId(
                    TicketCategory.RESTAURANT_CANCEL_SPIKE,
                    List.of(TicketStatus.NEW, TicketStatus.IN_PROGRESS),
                    restaurant.getId()
                );

                if (exists) {
                    continue;
                }

                Map<String, Object> evidence = new HashMap<>();
                evidence.put("restaurantId", restaurant.getId());
                evidence.put("restaurantName", restaurant.getName());
                evidence.put("windowHours", 24);
                evidence.put("totalOrders", total);
                evidence.put("cancelledOrders", cancelled);
                evidence.put("cancelRate", cancelRate);
                evidence.put("previousTotalOrders", prevTotal);
                evidence.put("previousCancelledOrders", prevCancelled);
                evidence.put("previousCancelRate", prevCancelRate);
                evidence.put("trendSeries", buildHourlySeries(restaurant.getId(), now));
                evidence.put("trendSeriesDaily", buildDailySeries(restaurant.getId(), now));

                String title = String.format("[Order Anomaly] %s cancel rate anomaly", restaurant.getName());
                String description = String.format(
                    "Cancel rate reached %.1f%% in the last 24 hours (cancelled %d / total %d).",
                    cancelRate * 100,
                    cancelled,
                    total
                );

                ticketService.createSystemTicket(
                    title,
                    description,
                    TicketCategory.RESTAURANT_CANCEL_SPIKE,
                    TicketPriority.HIGH,
                    restaurant.getId(),
                    ticketService.toEvidenceJson(evidence)
                );

                log.info("Anomaly ticket created: restaurantId={}, cancelRate={}", restaurant.getId(), cancelRate);
            }

            if (delivered >= MIN_SAMPLE_SIZE) {
                Long delayed = orderRepository.countRecentDelayedOrdersByRestaurant(restaurant.getId(), since);
                Long timeout = orderRepository.countRecentTimeoutOrdersByRestaurant(restaurant.getId(), since);
                Double avgDelayMinutes = orderRepository.avgDelayMinutesByRestaurant(restaurant.getId(), since);

                double delayRate = (delivered == 0)
                    ? 0
                    : (delayed == null ? 0 : delayed.doubleValue()) / delivered.doubleValue();
                double timeoutRate = (delivered == 0)
                    ? 0
                    : (timeout == null ? 0 : timeout.doubleValue()) / delivered.doubleValue();

                if (delayRate >= DELAY_RATE_THRESHOLD) {
                    boolean exists = ticketRepository.existsByCategoryAndStatusInAndRestaurantId(
                        TicketCategory.DELIVERY_DELAY_SPIKE,
                        List.of(TicketStatus.NEW, TicketStatus.IN_PROGRESS),
                        restaurant.getId()
                    );
                    if (!exists) {
                        Map<String, Object> evidence = new HashMap<>();
                        evidence.put("restaurantId", restaurant.getId());
                        evidence.put("restaurantName", restaurant.getName());
                        evidence.put("windowHours", 24);
                        evidence.put("deliveredOrders", delivered);
                        evidence.put("delayedOrders", delayed);
                        evidence.put("delayRate", delayRate);
                        evidence.put("avgDelayMinutes", avgDelayMinutes == null ? 0 : avgDelayMinutes);
                        evidence.put("trendSeries", buildHourlySeries(restaurant.getId(), now));
                        evidence.put("trendSeriesDaily", buildDailySeries(restaurant.getId(), now));

                        String title = String.format("[Delivery Anomaly] %s delivery delay anomaly", restaurant.getName());
                        String description = String.format(
                            "Delivery delay rate reached %.1f%% in the last 24 hours (delayed %d / delivered %d), average delay %.1f minutes.",
                            delayRate * 100,
                            delayed == null ? 0 : delayed,
                            delivered,
                            avgDelayMinutes == null ? 0 : avgDelayMinutes
                        );

                        ticketService.createSystemTicket(
                            title,
                            description,
                            TicketCategory.DELIVERY_DELAY_SPIKE,
                            TicketPriority.HIGH,
                            restaurant.getId(),
                            ticketService.toEvidenceJson(evidence)
                        );
                    }
                }

                if (timeoutRate >= TIMEOUT_RATE_THRESHOLD) {
                    boolean exists = ticketRepository.existsByCategoryAndStatusInAndRestaurantId(
                        TicketCategory.DELIVERY_TIMEOUT_SPIKE,
                        List.of(TicketStatus.NEW, TicketStatus.IN_PROGRESS),
                        restaurant.getId()
                    );
                    if (!exists) {
                        Map<String, Object> evidence = new HashMap<>();
                        evidence.put("restaurantId", restaurant.getId());
                        evidence.put("restaurantName", restaurant.getName());
                        evidence.put("windowHours", 24);
                        evidence.put("deliveredOrders", delivered);
                        evidence.put("timeoutOrders", timeout);
                        evidence.put("timeoutRate", timeoutRate);
                        evidence.put("avgDelayMinutes", avgDelayMinutes == null ? 0 : avgDelayMinutes);
                        evidence.put("trendSeries", buildHourlySeries(restaurant.getId(), now));
                        evidence.put("trendSeriesDaily", buildDailySeries(restaurant.getId(), now));

                        String title = String.format("[Delivery Anomaly] %s timeout orders spike", restaurant.getName());
                        String description = String.format(
                            "Timeout rate reached %.1f%% in the last 24 hours (timeout %d / delivered %d).",
                            timeoutRate * 100,
                            timeout == null ? 0 : timeout,
                            delivered
                        );

                        ticketService.createSystemTicket(
                            title,
                            description,
                            TicketCategory.DELIVERY_TIMEOUT_SPIKE,
                            TicketPriority.HIGH,
                            restaurant.getId(),
                            ticketService.toEvidenceJson(evidence)
                        );
                    }
                }
            }

            Long refunded = orderRepository.countRecentRefundedOrdersByRestaurant(
                restaurant.getId(),
                Order.PaymentStatus.REFUNDED,
                since
            );
            double refundRate = (total == 0)
                ? 0
                : (refunded == null ? 0 : refunded.doubleValue()) / total.doubleValue();

            if (refundRate >= REFUND_RATE_THRESHOLD) {
                boolean exists = ticketRepository.existsByCategoryAndStatusInAndRestaurantId(
                    TicketCategory.PAYMENT_REFUND_SPIKE,
                    List.of(TicketStatus.NEW, TicketStatus.IN_PROGRESS),
                    restaurant.getId()
                );
                if (!exists) {
                    Map<String, Object> evidence = new HashMap<>();
                    evidence.put("restaurantId", restaurant.getId());
                    evidence.put("restaurantName", restaurant.getName());
                    evidence.put("windowHours", 24);
                    evidence.put("totalOrders", total);
                    evidence.put("refundedOrders", refunded);
                    evidence.put("refundRate", refundRate);
                    evidence.put("trendSeries", buildHourlySeries(restaurant.getId(), now));
                    evidence.put("trendSeriesDaily", buildDailySeries(restaurant.getId(), now));

                    String title = String.format("[Payment Anomaly] %s refund rate anomaly", restaurant.getName());
                    String description = String.format(
                        "Refund rate reached %.1f%% in the last 24 hours (refunded %d / total %d).",
                        refundRate * 100,
                        refunded == null ? 0 : refunded,
                        total
                    );

                    ticketService.createSystemTicket(
                        title,
                        description,
                        TicketCategory.PAYMENT_REFUND_SPIKE,
                        TicketPriority.HIGH,
                        restaurant.getId(),
                        ticketService.toEvidenceJson(evidence)
                    );
                }
            }
        }
    }

    private List<Map<String, Object>> buildHourlySeries(Long restaurantId, LocalDateTime now) {
        List<Map<String, Object>> series = new ArrayList<>();
        for (int i = 23; i >= 0; i--) {
            LocalDateTime start = now.minusHours(i + 1);
            LocalDateTime end = now.minusHours(i);
            Long total = orderRepository.countOrdersByRestaurantBetween(restaurantId, start, end);
            Long cancelled = orderRepository.countOrdersByRestaurantAndStatusBetween(
                restaurantId,
                OrderStatus.CANCELLED,
                start,
                end
            );
            double rate = (total == null || total == 0)
                ? 0
                : (cancelled == null ? 0 : cancelled.doubleValue()) / total.doubleValue();

            Map<String, Object> point = new HashMap<>();
            point.put("start", start.toString());
            point.put("end", end.toString());
            point.put("total", total == null ? 0 : total);
            point.put("cancelled", cancelled == null ? 0 : cancelled);
            point.put("cancelRate", rate);
            series.add(point);
        }
        return series;
    }

    private List<Map<String, Object>> buildDailySeries(Long restaurantId, LocalDateTime now) {
        List<Map<String, Object>> series = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime start = now.minusDays(i + 1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime end = now.minusDays(i).withHour(0).withMinute(0).withSecond(0).withNano(0);
            Long total = orderRepository.countOrdersByRestaurantBetween(restaurantId, start, end);
            Long cancelled = orderRepository.countOrdersByRestaurantAndStatusBetween(
                restaurantId,
                OrderStatus.CANCELLED,
                start,
                end
            );
            double rate = (total == null || total == 0)
                ? 0
                : (cancelled == null ? 0 : cancelled.doubleValue()) / total.doubleValue();

            Map<String, Object> point = new HashMap<>();
            point.put("start", start.toLocalDate().toString());
            point.put("total", total == null ? 0 : total);
            point.put("cancelled", cancelled == null ? 0 : cancelled);
            point.put("cancelRate", rate);
            series.add(point);
        }
        return series;
    }
}
