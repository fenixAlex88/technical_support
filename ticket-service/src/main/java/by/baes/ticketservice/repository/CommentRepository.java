package by.baes.ticketservice.repository;

import by.baes.ticketservice.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTicketId(Long ticketId);
}