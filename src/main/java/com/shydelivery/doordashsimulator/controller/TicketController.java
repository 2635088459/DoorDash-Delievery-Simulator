package com.shydelivery.doordashsimulator.controller;

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
import com.shydelivery.doordashsimulator.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "工单管理", description = "异常工单与Agent建议 API")
@PreAuthorize("hasRole('ADMIN')")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "创建工单", description = "手动创建工单")
    public ResponseEntity<TicketDTO> createTicket(
        @Valid @RequestBody CreateTicketRequest request,
        Authentication authentication
    ) {
        String userEmail = authentication.getName();
        log.info("创建工单: user={}, category={}", userEmail, request.getCategory());
        return ResponseEntity.ok(ticketService.createTicket(request, userEmail));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取工单列表", description = "获取所有工单")
    public ResponseEntity<List<TicketDTO>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取工单详情", description = "获取工单详情与评论")
    public ResponseEntity<TicketDTO> getTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @GetMapping("/{id}/samples")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取异常订单样本", description = "获取工单关联的异常订单样本")
    public ResponseEntity<List<TicketSampleOrderDTO>> getSampleOrders(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getSampleOrders(id));
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "生成工单总结", description = "生成工单一键总结报告")
    public ResponseEntity<TicketSummaryDTO> getSummary(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.generateSummary(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "更新工单状态", description = "更新工单状态与分派信息")
    public ResponseEntity<TicketDTO> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateTicketStatusRequest request,
        Authentication authentication
    ) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(ticketService.updateStatus(id, request, userEmail));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "新增工单评论", description = "添加工单评论或Agent建议")
    public ResponseEntity<TicketCommentDTO> addComment(
        @PathVariable Long id,
        @Valid @RequestBody AddTicketCommentRequest request,
        Authentication authentication
    ) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(ticketService.addComment(id, request, userEmail));
    }

    @PostMapping("/{id}/actions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "执行Agent动作", description = "记录Agent执行动作并更新工单状态")
    public ResponseEntity<TicketDTO> executeAction(
        @PathVariable Long id,
        @Valid @RequestBody ExecuteTicketActionRequest request,
        Authentication authentication
    ) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(ticketService.executeAction(id, request, userEmail));
    }

    @GetMapping("/{id}/actions/logs")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取工单动作日志", description = "查看某工单的动作日志")
    public ResponseEntity<List<TicketActionLogDTO>> getActionLogs(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getActionLogs(id));
    }

    @GetMapping("/actions/logs")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取动作审计日志", description = "查看所有工单动作日志")
    public ResponseEntity<List<TicketActionLogDTO>> getAllActionLogs() {
        return ResponseEntity.ok(ticketService.getActionLogs(null));
    }

    @PatchMapping("/{id}/actions/{actionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "回写动作结果", description = "更新动作执行结果")
    public ResponseEntity<TicketActionLogDTO> updateActionResult(
        @PathVariable Long id,
        @PathVariable Long actionId,
        @Valid @RequestBody UpdateTicketActionResultRequest request,
        Authentication authentication
    ) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(ticketService.updateActionResult(id, actionId, request, userEmail));
    }
}
