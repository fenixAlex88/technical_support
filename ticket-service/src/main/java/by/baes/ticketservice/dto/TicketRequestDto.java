package by.baes.ticketservice.dto;

import by.baes.ticketservice.entity.Ticket;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Ticket request data")
public class TicketRequestDto {
    @Schema(description = "Title of the ticket", example = "Test")
    private String title;

    @Schema(description = "Description of the ticket", example = "Test desc")
    private String description;

    @Schema(description = "Priority of the ticket", example = "HIGH")
    private Ticket.Priority priority;

    @Schema(description = "Category of the ticket", example = "TECHNICAL")
    private Ticket.Category category;

    @Schema(description = "List of attachment file names", example = "[\"file1.jpg\"]")
    private List<String> attachments;

    @Schema(description = "Creation timestamp", example = "2025-03-18T08:29:47.140Z")
    private LocalDateTime createdAt;

    @Schema(description = "Update timestamp", example = "2025-03-18T08:29:47.140Z")
    private LocalDateTime updatedAt;

    @Schema(description = "User who created the ticket", example = "user123")
    private String createdBy;

    @Schema(description = "User who updated the ticket", example = "user123")
    private String updatedBy;
}