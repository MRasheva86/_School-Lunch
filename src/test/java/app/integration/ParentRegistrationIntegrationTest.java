package app.integration;

import app.parent.model.Parent;
import app.parent.model.ParentRole;
import app.parent.repository.ParentRepository;
import app.parent.service.ParentService;
import app.wallet.model.Wallet;
import app.wallet.repository.WalletRepository;
import app.web.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ParentRegistrationIntegrationTest {

    @Autowired
    private ParentService parentService;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private WalletRepository walletRepository;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {

        walletRepository.deleteAll();
        parentRepository.deleteAll();

        registerRequest = RegisterRequest.builder()
                .username("integrationtestuser")
                .password("password123")
                .email("integration@test.com")
                .firstName("Integration")
                .lastName("Test")
                .build();
    }

    @Test
    void shouldRegisterParentAndCreateWalletInDatabase() {

        Parent registeredParent = parentService.register(registerRequest);

        assertNotNull(registeredParent.getId());
        Optional<Parent> savedParent = parentRepository.findById(registeredParent.getId());
        assertTrue(savedParent.isPresent());
        assertEquals(registerRequest.getUsername(), savedParent.get().getUsername());
        assertEquals(registerRequest.getEmail(), savedParent.get().getEmail());
        assertEquals(registerRequest.getFirstName(), savedParent.get().getFirstName());
        assertEquals(registerRequest.getLastName(), savedParent.get().getLastName());
        assertEquals(ParentRole.ROLE_USER, savedParent.get().getRole());
        assertTrue(savedParent.get().isActive());
        assertNotNull(savedParent.get().getCreatedOn());
        assertNotNull(savedParent.get().getUpdatedOn());

        assertNotNull(savedParent.get().getWallet());
        Wallet wallet = savedParent.get().getWallet();
        assertNotNull(wallet.getId());
        assertEquals(BigDecimal.ZERO, wallet.getBalance());
        assertEquals(savedParent.get(), wallet.getOwner());

        Optional<Wallet> savedWallet = walletRepository.findById(wallet.getId());
        assertTrue(savedWallet.isPresent());
        assertEquals(wallet.getId(), savedWallet.get().getId());
        assertEquals(savedParent.get().getId(), savedWallet.get().getOwner().getId());
    }

    @Test
    void shouldThrowExceptionWhenRegisteringDuplicateUsername() {

        parentService.register(registerRequest);

        RegisterRequest duplicateRequest = RegisterRequest.builder()
                .username(registerRequest.getUsername())
                .password("differentpassword")
                .email("different@test.com")
                .firstName("Different")
                .lastName("User")
                .build();

        app.expetion.DomainException exception = assertThrows(app.expetion.DomainException.class, () -> {
            parentService.register(duplicateRequest);
        });

        assertEquals("This username is already registered. Please chose another one.", exception.getMessage());

        assertEquals(1, parentRepository.count());
    }

    @Test
    void shouldCreateWalletWithZeroBalance() {

        Parent registeredParent = parentService.register(registerRequest);

        Wallet wallet = registeredParent.getWallet();
        assertNotNull(wallet);
        assertEquals(BigDecimal.ZERO, wallet.getBalance());
        assertNotNull(wallet.getCurrency());
        assertNotNull(wallet.getCreatedOn());
        assertNotNull(wallet.getUpdatedOn());
    }

    @Test
    void shouldPersistParentAndWalletInSameTransaction() {

        Parent registeredParent = parentService.register(registerRequest);

        assertTrue(parentRepository.existsById(registeredParent.getId()));
        assertTrue(walletRepository.existsById(registeredParent.getWallet().getId()));

        Parent retrievedParent = parentRepository.findById(registeredParent.getId()).orElseThrow();
        Wallet retrievedWallet = walletRepository.findById(registeredParent.getWallet().getId()).orElseThrow();
        assertEquals(retrievedParent.getWallet().getId(), retrievedWallet.getId());
        assertEquals(retrievedWallet.getOwner().getId(), retrievedParent.getId());
    }

    @Test
    void shouldEncodePasswordWhenRegistering() {

        Parent registeredParent = parentService.register(registerRequest);

        Parent savedParent = parentRepository.findById(registeredParent.getId()).orElseThrow();
        assertNotEquals(registerRequest.getPassword(), savedParent.getPassword());
        assertTrue(savedParent.getPassword().startsWith("$2a$")); // BCrypt prefix
        assertTrue(savedParent.getPassword().length() > 50); // BCrypt hashed password length
    }

    @Test
    void shouldSetDefaultRoleToRoleUser() {

        Parent registeredParent = parentService.register(registerRequest);

        Parent savedParent = parentRepository.findById(registeredParent.getId()).orElseThrow();
        assertEquals(ParentRole.ROLE_USER, savedParent.getRole());
    }

    @Test
    void shouldSetIsActiveToTrue() {

        Parent registeredParent = parentService.register(registerRequest);

        Parent savedParent = parentRepository.findById(registeredParent.getId()).orElseThrow();
        assertTrue(savedParent.isActive());
    }

    @Test
    void shouldAllowMultipleParentsWithDifferentUsernames() {

        RegisterRequest request1 = RegisterRequest.builder()
                .username("user1")
                .password("password1")
                .email("user1@test.com")
                .firstName("User")
                .lastName("One")
                .build();

        RegisterRequest request2 = RegisterRequest.builder()
                .username("user2")
                .password("password2")
                .email("user2@test.com")
                .firstName("User")
                .lastName("Two")
                .build();

        Parent parent1 = parentService.register(request1);
        Parent parent2 = parentService.register(request2);

        assertEquals(2, parentRepository.count());
        assertEquals(2, walletRepository.count());
        assertNotEquals(parent1.getId(), parent2.getId());
        assertNotEquals(parent1.getWallet().getId(), parent2.getWallet().getId());
    }
}

