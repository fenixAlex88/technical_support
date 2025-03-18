package by.baes.ticketservice.controller;

import by.baes.ticketservice.entity.Comment;
import by.baes.ticketservice.entity.Ticket;
import by.baes.ticketservice.entity.TicketHistory;
import by.baes.ticketservice.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class TicketController {
    private final TicketService ticketService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Create a new ticket", description = "Creates a ticket with optional file attachments")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Ticket> createTicket(
            @RequestPart(value = "ticket", required = true)
            @Schema(description = "Ticket data in JSON format",
                    example = "{\"title\":\"Test\",\"description\":\"Test desc\",\"priority\":\"HIGH\",\"category\":\"TECHNICAL\"}")
            String ticketJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestHeader("X-User-Id") String userId) {
        try {
            // Проверяем, что строка не пустая и начинается с '{', чтобы избежать невалидного JSON
            if (ticketJson == null || ticketJson.trim().isEmpty() || !ticketJson.trim().startsWith("{")) {
                log.warn("Invalid ticket JSON: {}", ticketJson);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            Ticket ticket = objectMapper.readValue(ticketJson, Ticket.class);
            Ticket createdTicket = ticketService.createTicket(ticket, files, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTicket);
        } catch (IOException e) {
            log.error("Failed to parse ticket JSON: {}", ticketJson, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Operation(summary = "Get a ticket by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicket(id));
    }

    @Operation(summary = "Update a ticket", description = "Updates a ticket with optional additional file attachments")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Ticket> updateTicket(
            @PathVariable Long id,
            @RequestPart(value = "ticket", required = true)
            @Schema(description = "Ticket data in JSON format",
                    example = "{\"title\":\"Updated Test\",\"description\":\"Updated desc\",\"priority\":\"LOW\",\"category\":\"SUPPORT\"}")
            String ticketJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestHeader("X-User-Id") String userId) {
        try {
            if (ticketJson == null || ticketJson.trim().isEmpty() || !ticketJson.trim().startsWith("{")) {
                log.warn("Invalid ticket JSON: {}", ticketJson);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            Ticket ticket = objectMapper.readValue(ticketJson, Ticket.class);
            return ResponseEntity.ok(ticketService.updateTicket(id, ticket, files, userId));
        } catch (IOException e) {
            log.error("Failed to parse ticket JSON: {}", ticketJson, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Operation(summary = "Get ticket history")
    @GetMapping("/{id}/history")
    public ResponseEntity<List<TicketHistory>> getTicketHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketHistory(id));
    }

    @Operation(summary = "Add a comment to a ticket")
    @PostMapping("/{id}/comments")
    public ResponseEntity<Comment> addComment(
            @PathVariable Long id,
            @RequestBody String content,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.addComment(id, content, userId));
    }

    @Operation(summary = "Get comments for a ticket")
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<Comment>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getComments(id));
    }
}