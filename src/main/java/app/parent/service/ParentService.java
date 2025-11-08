package app.parent.service;

import app.child.model.Child;
import app.child.repository.ChildRepository;
import app.expetion.DomainExeption;
import app.parent.model.Parent;
import app.parent.model.ParentRole;
import app.parent.repository.ParentRepository;
import app.security.UserData;
import app.wallet.model.Wallet;
import app.wallet.repository.WalletRepository;
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
    private final WalletRepository walletRepository;
    private RegisterRequest registerRequest;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Parent parent = parentRepository.findByUsername(username)
                .orElseThrow(() -> new DomainExeption("Parent with username [%s] not found.".formatted(username)));
        return new UserData(parent.getId(), username, parent.getPassword(), parent.isActive(), parent.getRole());
    }

    private final ParentRepository parentRepository;
    private final PasswordEncoder passwordEncoder;
    private final ChildRepository childRepository;
    private final WalletService walletService;

    @Autowired
    public ParentService(ParentRepository parentRepository, PasswordEncoder passwordEncoder, ChildRepository childRepository, WalletService walletService, WalletRepository walletRepository) {
        this.parentRepository = parentRepository;
        this.passwordEncoder = passwordEncoder;
        this.childRepository = childRepository;
        this.walletService = walletService;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public Parent register(RegisterRequest registerRequest) {
        this.registerRequest = registerRequest;

        Optional<Parent> optionalParent = parentRepository.findByUsername(registerRequest.getUsername());
        if (optionalParent.isPresent()) {
            throw new DomainExeption("Parent with [%s] username already exist.".formatted(registerRequest.getUsername()));
        }

        Parent parent = Parent.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .email(registerRequest.getEmail())
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .role(registerRequest.getRole())
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
        return parentRepository.findById(id).orElseThrow(() -> new DomainExeption("Parent by id [%s] was not found.".formatted(id)));
    }

    public Parent findByUsername(String username) {
        return parentRepository.findByUsername(username).orElseThrow(() -> new DomainExeption("Parent with username [%s] not found.".formatted(username)));
    }

    public void updateProfile(UUID parentId, EditRequest editRequest) {
        Parent parent = getById(parentId);
        parent.setEmail(editRequest.getEmail());
        parent.setPassword(passwordEncoder.encode(editRequest.getPassword()));
        parent.setRole(editRequest.getRole());
        parent.setUpdatedOn(LocalDateTime.now());
        parentRepository.save(parent);
    }

    @Cacheable("users")
    public List<Parent> getAllParents() {
        return parentRepository.findAll();
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
