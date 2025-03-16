package by.baes.authservice.repository;

import by.baes.authservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByName(String name);
    boolean existsByName(String name);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name IN :roleNames")
    Page<User> findByRolesIn(List<String> roleNames, Pageable pageable);
}