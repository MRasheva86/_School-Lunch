package app.lunch.repository;

import app.lunch.model.Lunch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LunchRepository extends JpaRepository<Lunch, UUID> {

    List<Lunch> findByChildIdOrderByWeekDayDesc(UUID childId);
}
