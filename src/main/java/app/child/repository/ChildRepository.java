package app.child.repository;

import app.child.model.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChildRepository extends JpaRepository<Child, UUID> {
    Child findChildByFirstName(String firstName);

 //   @Query("SELECT c FROM Child c WHERE c.parent.id = :parentId")
    List<Child> findByParentId(@Param("parentId") UUID parentId);
}
