package app.parent.service;

import app.child.model.Child;
import app.child.repository.ChildRepository;
import app.expetion.DomainExeption;
import app.parent.model.Parent;
import app.parent.model.ParentRole;
import app.parent.repository.ParentRepository;
import app.security.UserData;
import app.wallet.model.Wallet;
import app.wallet.service.WalletService;
import app.web.dto.EditRequest;
import app.web.dto.RegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ParentService implements UserDetailsService {
    private final ParentRepository parentRepository;
    private final PasswordEncoder passwordEncoder;
    private final ChildRepository childRepository;
    private final WalletService walletService;

    @Autowired
    public ParentService(ParentRepository parentRepository, PasswordEncoder passwordEncoder, ChildRepository childRepository, WalletService walletService) {
        this.parentRepository = parentRepository;
        this.passwordEncoder = passwordEncoder;
        this.childRepository = childRepository;
        this.walletService = walletService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Parent parent = parentRepository.findByUsername(username)
                .orElseThrow(() -> new DomainExeption("Parent with username [%s] not found.".formatted(username)));
        return new UserData(parent.getId(), username, parent.getPassword(), parent.isActive(), parent.getRole());
    }

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public Parent register(RegisterRequest registerRequest) {
        Optional<Parent> optionalParent = parentRepository.findByUsername(registerRequest.getUsername());
        if (optionalParent.isPresent()) {
            throw new DomainExeption("This username is already registered. Please chose another one.");
        }

        Parent parent = Parent.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .email(registerRequest.getEmail())
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .role(ParentRole.ROLE_USER)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();


        Wallet wallet = walletService.createWallet(parent);
        parent.setWallet(wallet);
        parentRepository.save(parent);
        return parent;
    }

    public List<Child> getChildren(Parent parent) {
        return childRepository.findAll();
    }

    public Parent getById(UUID id) {
        Parent parent = parentRepository.findById(id).orElseThrow(() -> new DomainExeption("Parent by id [%s] was not found.".formatted(id)));
        // Ensure role is set (default to ROLE_USER if null)
        if (parent.getRole() == null) {
            parent.setRole(ParentRole.ROLE_USER);
            parentRepository.save(parent);
        }
        return parent;
    }

    public Parent findByUsername(String username) {
        Parent parent = parentRepository.findByUsername(username).orElseThrow(() -> new DomainExeption("Parent with username [%s] not found.".formatted(username)));
        // Ensure role is set (default to ROLE_USER if null)
        if (parent.getRole() == null) {
            parent.setRole(ParentRole.ROLE_USER);
            parentRepository.save(parent);
        }
        return parent;
    }

    public void updateProfile(UUID parentId, EditRequest editRequest) {
        Parent parent = getById(parentId);
        parent.setEmail(editRequest.getEmail());
        parent.setPassword(passwordEncoder.encode(editRequest.getPassword()));
        // Ensure role is never null - default to ROLE_USER if not provided
        parent.setRole(editRequest.getRole() != null ? editRequest.getRole() : ParentRole.ROLE_USER);
        parent.setUpdatedOn(LocalDateTime.now());
        parentRepository.save(parent);
    }

    @Transactional
    @Cacheable(value = "users", key = "'all'")
    public List<Parent> getAllParents() {
        List<Parent> parents = parentRepository.findAll();
        // Ensure all parents have a valid role (default to ROLE_USER if null or invalid)
        // The converter should handle invalid values, but we ensure it here too
        boolean needsSave = false;
        for (Parent parent : parents) {
            if (parent.getRole() == null) {
                parent.setRole(ParentRole.ROLE_USER);
                needsSave = true;
            }
        }
        if (needsSave) {
            parentRepository.saveAll(parents);
        }
        return parents;
    }

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void deleteParent(UUID userId) {
        Wallet wallet = walletService.getWalletByParentId(userId);
        if (wallet != null) {
            walletService.deleteWallet(wallet.getId());
        }
        parentRepository.deleteById(userId);
    }

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void updateUserRole(UUID userId, String newRole) {

        Parent parent = parentRepository.findById(userId)
                .orElseThrow(() -> new DomainExeption("User not found"));

        parent.setRole(ParentRole.valueOf(newRole));

        parentRepository.save(parent);
    }
}
