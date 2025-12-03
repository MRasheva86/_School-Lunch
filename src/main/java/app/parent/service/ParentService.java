package app.parent.service;

import app.expetion.DomainException;
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
    private final WalletService walletService;

    @Autowired
    public ParentService(ParentRepository parentRepository, PasswordEncoder passwordEncoder, WalletService walletService) {
        this.parentRepository = parentRepository;
        this.passwordEncoder = passwordEncoder;
        this.walletService = walletService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Parent parent = parentRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("Parent with username [%s] not found.".formatted(username)));

        return new UserData(parent.getId(), username, parent.getPassword(), parent.isActive(), parent.getRole());
    }

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public Parent register(RegisterRequest registerRequest) {

        Optional<Parent> optionalParent = parentRepository.findByUsername(registerRequest.getUsername());

        if (optionalParent.isPresent()) {

            log.warn("Registration failed: username {} already exists", registerRequest.getUsername());
            throw new DomainException("This username is already registered. Please chose another one.");

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
        Parent savedParent = parentRepository.save(parent);

        log.info("Successfully registered parent: {} with id: {}", savedParent.getUsername(), savedParent.getId());

        return savedParent;
    }

    public Parent getById(UUID id) {

        return parentRepository.findById(id)
                .orElseThrow(() -> new DomainException("Parent by id [%s] was not found.".formatted(id)));
    }

    public Parent findByUsername(String username) {

        return parentRepository.findByUsername(username)
                .orElseThrow(() -> new DomainException("Parent with username [%s] not found.".formatted(username)));
    }

    public void updateProfile(UUID parentId, EditRequest editRequest) {
        Parent parent = getById(parentId);
        parent.setEmail(editRequest.getEmail());
        parent.setPassword(passwordEncoder.encode(editRequest.getPassword()));
        parent.setRole(editRequest.getRole() != null ? editRequest.getRole() : ParentRole.ROLE_USER);
        parent.setUpdatedOn(LocalDateTime.now());
        parentRepository.save(parent);
        log.info("Successfully updated profile for parent: {}", parentId);
    }

    public EditRequest createEditRequest(Parent parent) {
        return EditRequest.builder()
                .email(parent.getEmail())
                .role(parent.getRole())
                .build();
    }

    @Transactional
    @Cacheable(value = "users", key = "'all'")
    public List<Parent> getAllParents() {

        List<Parent> parents = parentRepository.findAll();
        log.debug("Retrieved {} parents", parents.size());
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

        log.info("Successfully deleted parent: {}", userId);
    }

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void updateUserRole(UUID userId, String newRole) {

        Parent parent = parentRepository.findById(userId)
                .orElseThrow(() -> new DomainException("User not found"));

        parent.setRole(ParentRole.valueOf(newRole));

        parentRepository.save(parent);

        log.info("Successfully updated role for user: {} to {}", userId, newRole);
    }
}
