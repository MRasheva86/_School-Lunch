package app.child.service;

import app.child.model.Child;
import app.child.repository.ChildRepository;
import app.expetion.DomainExeption;
import app.lunch.client.dto.LunchOrder;
import app.lunch.service.LunchService;
import app.parent.model.Parent;
import app.parent.service.ParentService;
import app.web.dto.ChildRequest;
import app.web.dto.EditChildRequest;
import app.wallet.model.Wallet;
import app.wallet.service.WalletService;
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

@Service
public class ChildService {

    private final ChildRepository childRepository;
    private final ParentService parentService;
    private final LunchService lunchService;
    private final WalletService walletService;
    
    @Value("${app.upload.dir:src/main/resources/static/images/children}")
    private String uploadDir;

    public ChildService(ChildRepository childRepository, ParentService parentService, 
                       LunchService lunchService, WalletService walletService) {
        this.childRepository = childRepository;
        this.parentService = parentService;
        this.lunchService = lunchService;
        this.walletService = walletService;
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

    @Transactional
    public void deleteChild(UUID childId) {
        Child child = getChildById(childId);
        Parent parent = child.getParent();
        
        // Get all lunches for this child (including paid ones)
        List<LunchOrder> lunches = lunchService.getAllLunchesIncludingDeleted(childId);
        
        // Filter for paid lunches and calculate total refund amount
        BigDecimal totalRefund = BigDecimal.ZERO;
        for (LunchOrder lunch : lunches) {
            String status = lunch.getStatus();
            // Only refund lunches with status "PAID" (completed lunches should not be refunded)
            if (status != null && status.equalsIgnoreCase("PAID")) {
                if (lunch.getTotal() != null) {
                    totalRefund = totalRefund.add(lunch.getTotal());
                }
            }
        }
        
        // Refund money to parent's wallet if there are paid lunches
        if (totalRefund.compareTo(BigDecimal.ZERO) > 0) {
            Wallet wallet = walletService.getWalletByParentId(parent.getId());
            if (wallet == null) {
                wallet = walletService.createWallet(parent);
            }
            walletService.deposit(wallet.getId(), totalRefund, 
                "Refund for deleted child: " + child.getFirstName() + " " + child.getLastName());
        }
        
        // Delete the child
        childRepository.deleteById(childId);
    }

    public Child getChildById(UUID childId) {
        return childRepository.findById(childId).orElseThrow(() ->new DomainExeption("Child not found"));
    }

    public void updateProfile(UUID childId, EditChildRequest editChildRequest) {
        Child child = getChildById(childId);
        child.setSchool(editChildRequest.getSchool());
        child.setGrade(editChildRequest.getGrade());
        
        // Handle image upload if provided
        if (editChildRequest.getImage() != null && !editChildRequest.getImage().isEmpty()) {
            try {
                String imagePath = saveImage(editChildRequest.getImage(), childId);
                child.setImagePath(imagePath);
            } catch (IOException e) {
                throw new DomainExeption("Failed to save image: " + e.getMessage());
            }
        }
        
        childRepository.save(child);
    }
    
    private String saveImage(MultipartFile file, UUID childId) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename: child-{childId}-{timestamp}.{extension}
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = "child-" + childId + "-" + System.currentTimeMillis() + extension;
        
        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Return relative path for web access
        return "/images/children/" + filename;
    }
}
