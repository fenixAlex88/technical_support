package by.baes.ticketservice.repository;

import by.baes.ticketservice.entity.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {
    List<TicketHistory> findByTicketId(Long ticketId);
}