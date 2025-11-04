package app.child.service;

import app.child.model.Child;
import app.child.repository.ChildRepository;
import app.expetion.DomainExeption;
import app.parent.service.ParentService;
import app.web.dto.ChildRequest;
import app.web.dto.EditChildRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ChildService {

    private final ChildRepository childRepository;
    private final ParentService parentService;

    public ChildService(ChildRepository childRepository, ParentService parentService) {
        this.childRepository = childRepository;
        this.parentService = parentService;
    }

    public Child getChildByName( String childName) {
        return childRepository.findChildByFirstName(childName);

    }

    public List<Child> getChildrenByParentId(UUID id) {
       return childRepository.findByParentId(id);
    }

    public Child registerChild(UUID parentId, ChildRequest childRequest) {
        Child child = Child.builder()
                .firstName(childRequest.getFirstName())
                .lastName(childRequest.getLastName())
                .parent(parentService.getById(parentId))
                .school(childRequest.getSchool())
                .grade(childRequest.getGrade())
                .gender(childRequest.getGender())
                .build();
        return childRepository.save(child);
    }

    public void deleteChild(UUID childId) {

        childRepository.deleteById(childId);
    }

    public Child getChildById(UUID childId) {
        return childRepository.findById(childId).orElseThrow(() ->new DomainExeption("Child not found"));
    }

    public void updateProfile(UUID childId, EditChildRequest editChildRequest) {
        Child child = getChildById(childId);
        child.setSchool(editChildRequest.getSchool());
        child.setGrade(editChildRequest.getGrade());
        childRepository.save(child);
    }
}
