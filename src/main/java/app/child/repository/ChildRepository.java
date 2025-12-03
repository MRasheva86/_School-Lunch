package app.child.repository;

import app.child.model.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChildRepository extends JpaRepository<Child, UUID> {

    List<Child> findByParentId(@Param("parentId") UUID parentId);
}
