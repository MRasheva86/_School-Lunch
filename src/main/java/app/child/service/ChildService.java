package app.child.service;

import app.child.model.Child;
import app.child.repository.ChildRepository;
import app.expetion.DomainException;
import app.lunch.client.dto.LunchOrder;
import app.lunch.service.LunchService;
import app.parent.model.Parent;
import app.parent.service.ParentService;
import app.web.dto.ChildRequest;
import app.web.dto.EditChildRequest;
import app.wallet.model.Wallet;
import app.wallet.service.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ChildService {

    private final ChildRepository childRepository;
    private final ParentService parentService;
    private final LunchService lunchService;
    private final WalletService walletService;
    
    @Value("${app.upload.dir:src/main/resources/static/images/children}")
    private String personalImg;

    public ChildService(ChildRepository childRepository, ParentService parentService, 
                       LunchService lunchService, WalletService walletService) {
        this.childRepository = childRepository;
        this.parentService = parentService;
        this.lunchService = lunchService;
        this.walletService = walletService;
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
        Child savedChild = childRepository.save(child);

        log.info("Successfully registered child: {} with id: {}", savedChild.getFirstName(), savedChild.getId());
        return savedChild;
    }

    @Transactional
    public void deleteChild(UUID childId) {

        Child child = getChildById(childId);
        Parent parent = child.getParent();

        List<LunchOrder> lunches = lunchService.getAllLunchesIncludingDeleted(childId);
        
        BigDecimal totalRefund = BigDecimal.ZERO;

        for (LunchOrder lunch : lunches) {

            String status = lunch.getStatus();
            if (status != null && status.equalsIgnoreCase("PAID")) {
                totalRefund = totalRefund.add(lunch.getTotal());
            }
        }
        
        if (totalRefund.compareTo(BigDecimal.ZERO) > 0) {

            Wallet wallet = walletService.getWalletByParentId(parent.getId());
            if (wallet == null) {
                wallet = walletService.createWallet(parent);
            }
            walletService.deposit(wallet.getId(), totalRefund, 
                "Refund for deleted child: " + child.getFirstName() + " " + child.getLastName());
        }
        
        childRepository.deleteById(childId);

        log.info("Successfully deleted child: {} ({})", child.getFirstName(), childId);
    }

    public Child getChildById(UUID childId) {
        return childRepository.findById(childId).orElseThrow(() ->new DomainException("Child not found"));
    }

    public void ensureChildOwnership(UUID parentId, UUID childId) {

        Child child = getChildById(childId);

        if (!child.getParent().getId().equals(parentId)) {
            throw new DomainException("You can access lunches only for your own children.");
        }
    }

    public EditChildRequest createEditChildRequest(Child child) {

        EditChildRequest editChildRequest = new EditChildRequest();
        editChildRequest.setSchool(child.getSchool());
        editChildRequest.setGrade(child.getGrade());

        return editChildRequest;

    }

    public void updateProfile(UUID childId, EditChildRequest editChildRequest) {

        Child child = getChildById(childId);
        child.setSchool(editChildRequest.getSchool());
        child.setGrade(editChildRequest.getGrade());
        
        if (editChildRequest.getImage() != null && !editChildRequest.getImage().isEmpty()) {
            try {

                String imagePath = saveImage(editChildRequest.getImage(), childId);
                child.setImagePath(imagePath);

                log.info("Successfully saved image for child: {} at path: {}", childId, imagePath);
            } catch (IOException e) {
                log.error("Failed to save image for child: {}", childId, e);

                throw new DomainException("Failed to save image: " + e.getMessage());
            }
        }

        childRepository.save(child);

        log.info("Successfully updated profile for child: {}", childId);
    }
    
    private String saveImage(MultipartFile file, UUID childId) throws IOException {

        Path uploadPath = Paths.get(personalImg);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = "child-" + childId + "-" + System.currentTimeMillis() + extension;
        
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/images/children/" + filename;
    }
}
