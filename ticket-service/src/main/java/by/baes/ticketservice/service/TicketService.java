package by.baes.ticketservice.service;

import by.baes.ticketservice.entity.Comment;
import by.baes.ticketservice.entity.Ticket;
import by.baes.ticketservice.entity.TicketHistory;
import by.baes.ticketservice.repository.CommentRepository;
import by.baes.ticketservice.repository.TicketHistoryRepository;
import by.baes.ticketservice.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {
    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final CommentRepository commentRepository;
    private final RestTemplate restTemplate; // Заменяем WebClient на RestTemplate

    @Value("${photo-s3.url}")
    private String photoS3Url;

    @Transactional
    public Ticket createTicket(Ticket ticket, List<MultipartFile> files, String userId) {
        if (ticket.getTitle() == null || ticket.getTitle().trim().isEmpty()) {
            log.warn("Attempt to create ticket with empty title");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title cannot be empty");
        }

        List<String> attachments = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                String fileName = uploadFileToS3(file);
                attachments.add(fileName);
            }
        }

        ticket.setAttachments(attachments);
        ticket.setCreatedBy(userId);
        ticket.setUpdatedBy(userId);
        Ticket savedTicket = ticketRepository.save(ticket);

        log.info("Ticket created: id={}, title={}, by={}", savedTicket.getId(), savedTicket.getTitle(), userId);
        addHistory(savedTicket.getId(), "Ticket created", userId);
        return savedTicket;
    }

    public Ticket getTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Ticket not found: id={}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found");
                });
    }

    @Transactional
    public Ticket updateTicket(Long id, Ticket updatedTicket, List<MultipartFile> files, String userId) {
        Ticket existingTicket = getTicket(id);

        existingTicket.setTitle(updatedTicket.getTitle());
        existingTicket.setDescription(updatedTicket.getDescription());
        existingTicket.setPriority(updatedTicket.getPriority());
        existingTicket.setCategory(updatedTicket.getCategory());
        existingTicket.setUpdatedBy(userId);

        if (files != null && !files.isEmpty()) {
            List<String> attachments = existingTicket.getAttachments() != null ? new ArrayList<>(existingTicket.getAttachments()) : new ArrayList<>();
            for (MultipartFile file : files) {
                String fileName = uploadFileToS3(file);
                attachments.add(fileName);
            }
            existingTicket.setAttachments(attachments);
        }

        Ticket savedTicket = ticketRepository.save(existingTicket);
        log.info("Ticket updated: id={}, by={}", id, userId);
        addHistory(id, "Ticket updated", userId);
        return savedTicket;
    }

    public List<TicketHistory> getTicketHistory(Long ticketId) {
        return ticketHistoryRepository.findByTicketId(ticketId);
    }

    @Transactional
    public Comment addComment(Long ticketId, String content, String userId) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("Attempt to add empty comment to ticket: id={}", ticketId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment content cannot be empty");
        }

        getTicket(ticketId); // Проверка существования заявки

        Comment comment = new Comment();
        comment.setTicketId(ticketId);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setCreatedBy(userId);

        Comment savedComment = commentRepository.save(comment);
        log.info("Comment added to ticket: id={}, commentId={}", ticketId, savedComment.getId());
        return savedComment;
    }

    public List<Comment> getComments(Long ticketId) {
        return commentRepository.findByTicketId(ticketId);
    }

    private String uploadFileToS3(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartResource(file));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            return restTemplate.postForObject(photoS3Url, requestEntity, String.class);
        } catch (Exception e) {
            log.error("Failed to upload file to S3: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file to S3", e);
        }
    }

    private void addHistory(Long ticketId, String changeDescription, String userId) {
        TicketHistory history = new TicketHistory();
        history.setTicketId(ticketId);
        history.setChangeDescription(changeDescription);
        history.setChangedAt(LocalDateTime.now());
        history.setChangedBy(userId);
        ticketHistoryRepository.save(history);
    }

    // Вспомогательный класс для передачи MultipartFile через RestTemplate
    private static class MultipartResource extends ByteArrayResource {
        private final String filename;

        public MultipartResource(MultipartFile file) throws Exception {
            super(file.getBytes());
            this.filename = file.getOriginalFilename();
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}