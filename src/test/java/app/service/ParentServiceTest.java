package app.service;

import app.expetion.DomainException;
import app.parent.model.Parent;
import app.parent.model.ParentRole;
import app.parent.repository.ParentRepository;
import app.parent.service.ParentService;
import app.wallet.model.Wallet;
import app.wallet.service.WalletService;
import app.web.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParentServiceTest {

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private ParentService parentService;

    private RegisterRequest registerRequest;
    private Parent existingParent;
    private Wallet wallet;

    @BeforeEach
    void setUp() {

        registerRequest = RegisterRequest.builder()
                .username("testUser")
                .password("password123")
                .email("test@example.com")
                .firstName("Petar")
                .lastName("Ivanov")
                .build();

        existingParent = Parent.builder()
                .id(UUID.randomUUID())
                .username("testUser")
                .build();

        wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .build();
    }

    @Test
    void shouldThrowExceptionWhenUsernameAlreadyExists() {

        when(parentRepository.findByUsername(registerRequest.getUsername()))
                .thenReturn(Optional.of(existingParent));

        DomainException exception = assertThrows(DomainException.class, () -> {
            parentService.register(registerRequest);
        });

        assertEquals("This username is already registered. Please chose another one.", exception.getMessage());
        verify(parentRepository, times(1)).findByUsername(registerRequest.getUsername());
        verify(parentRepository, never()).save(any(Parent.class));
        verify(walletService, never()).createWallet(any(Parent.class));
    }

    @Test
    void shouldEncodePasswordWhenRegistering() {

        String encodedPassword = "encoded_password_123";

        when(parentRepository.findByUsername(registerRequest.getUsername()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword()))
                .thenReturn(encodedPassword);
        when(walletService.createWallet(any(Parent.class)))
                .thenReturn(wallet);
        when(parentRepository.save(any(Parent.class)))
                .thenAnswer(invocation -> {
                    Parent parent = invocation.getArgument(0);
                    parent.setId(UUID.randomUUID());
                    return parent;
                });

        parentService.register(registerRequest);

        verify(passwordEncoder, times(1)).encode(registerRequest.getPassword());

        ArgumentCaptor<Parent> parentCaptor = ArgumentCaptor.forClass(Parent.class);

        verify(parentRepository).save(parentCaptor.capture());
        assertEquals(encodedPassword, parentCaptor.getValue().getPassword());
    }

    @Test
    void shouldBuildParentEntityWithCorrectValues() {

        when(parentRepository.findByUsername(registerRequest.getUsername()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword()))
                .thenReturn("encoded_password");
        when(walletService.createWallet(any(Parent.class)))
                .thenReturn(wallet);
        when(parentRepository.save(any(Parent.class)))
                .thenAnswer(invocation -> {
                    Parent parent = invocation.getArgument(0);
                    parent.setId(UUID.randomUUID());
                    return parent;
                });

        parentService.register(registerRequest);

        ArgumentCaptor<Parent> parentCaptor = ArgumentCaptor.forClass(Parent.class);

        verify(parentRepository).save(parentCaptor.capture());

        Parent savedParent = parentCaptor.getValue();

        assertEquals(registerRequest.getUsername(), savedParent.getUsername());
        assertEquals(registerRequest.getEmail(), savedParent.getEmail());
        assertEquals(registerRequest.getFirstName(), savedParent.getFirstName());
        assertEquals(registerRequest.getLastName(), savedParent.getLastName());
        assertEquals(ParentRole.ROLE_USER, savedParent.getRole());
        assertTrue(savedParent.isActive());
        assertNotNull(savedParent.getCreatedOn());
        assertNotNull(savedParent.getUpdatedOn());
    }

    @Test
    void shouldSetDefaultRoleToRoleUser() {

        when(parentRepository.findByUsername(registerRequest.getUsername()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword()))
                .thenReturn("encoded_password");
        when(walletService.createWallet(any(Parent.class)))
                .thenReturn(wallet);
        when(parentRepository.save(any(Parent.class)))
                .thenAnswer(invocation -> {
                    Parent parent = invocation.getArgument(0);
                    parent.setId(UUID.randomUUID());
                    return parent;
                });

        parentService.register(registerRequest);

        ArgumentCaptor<Parent> parentCaptor = ArgumentCaptor.forClass(Parent.class);

        verify(parentRepository).save(parentCaptor.capture());

        assertEquals(ParentRole.ROLE_USER, parentCaptor.getValue().getRole());
    }

    @Test
    void shouldSetIsActiveToTrue() {

        when(parentRepository.findByUsername(registerRequest.getUsername()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword()))
                .thenReturn("encoded_password");
        when(walletService.createWallet(any(Parent.class)))
                .thenReturn(wallet);
        when(parentRepository.save(any(Parent.class)))
                .thenAnswer(invocation -> {
                    Parent parent = invocation.getArgument(0);
                    parent.setId(UUID.randomUUID());
                    return parent;
                });

        parentService.register(registerRequest);

        ArgumentCaptor<Parent> parentCaptor = ArgumentCaptor.forClass(Parent.class);

        verify(parentRepository).save(parentCaptor.capture());

        assertTrue(parentCaptor.getValue().isActive());
    }

    @Test
    void shouldSetTimestampsWhenRegistering() {

        LocalDateTime beforeRegistration = LocalDateTime.now();

        when(parentRepository.findByUsername(registerRequest.getUsername()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword()))
                .thenReturn("encoded_password");
        when(walletService.createWallet(any(Parent.class)))
                .thenReturn(wallet);
        when(parentRepository.save(any(Parent.class)))
                .thenAnswer(invocation -> {
                    Parent parent = invocation.getArgument(0);
                    parent.setId(UUID.randomUUID());
                    return parent;
                });

        parentService.register(registerRequest);

        ArgumentCaptor<Parent> parentCaptor = ArgumentCaptor.forClass(Parent.class);

        verify(parentRepository).save(parentCaptor.capture());

        Parent savedParent = parentCaptor.getValue();
        
        assertNotNull(savedParent.getCreatedOn());
        assertNotNull(savedParent.getUpdatedOn());
        assertTrue(savedParent.getCreatedOn().isAfter(beforeRegistration.minusSeconds(1)));
        assertTrue(savedParent.getUpdatedOn().isAfter(beforeRegistration.minusSeconds(1)));
    }

    @Test
    void shouldCreateWalletForParent() {

        when(parentRepository.findByUsername(registerRequest.getUsername()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword()))
                .thenReturn("encoded_password");
        when(walletService.createWallet(any(Parent.class)))
                .thenReturn(wallet);
        when(parentRepository.save(any(Parent.class)))
                .thenAnswer(invocation -> {
                    Parent parent = invocation.getArgument(0);
                    parent.setId(UUID.randomUUID());
                    return parent;
                });

        parentService.register(registerRequest);

        ArgumentCaptor<Parent> parentCaptor = ArgumentCaptor.forClass(Parent.class);

        verify(walletService, times(1)).createWallet(parentCaptor.capture());

        Parent parentForWallet = parentCaptor.getValue();

        assertEquals(registerRequest.getUsername(), parentForWallet.getUsername());
        assertEquals(registerRequest.getEmail(), parentForWallet.getEmail());
    }

    @Test
    void shouldSetWalletToParentBeforeSaving() {

        when(parentRepository.findByUsername(registerRequest.getUsername()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword()))
                .thenReturn("encoded_password");
        when(walletService.createWallet(any(Parent.class)))
                .thenReturn(wallet);
        when(parentRepository.save(any(Parent.class)))
                .thenAnswer(invocation -> {
                    Parent parent = invocation.getArgument(0);
                    parent.setId(UUID.randomUUID());
                    return parent;
                });

        parentService.register(registerRequest);

        ArgumentCaptor<Parent> parentCaptor = ArgumentCaptor.forClass(Parent.class);

        verify(parentRepository).save(parentCaptor.capture());

        assertEquals(wallet, parentCaptor.getValue().getWallet());
    }

    @Test
    void shouldSaveParentToRepository() {

        when(parentRepository.findByUsername(registerRequest.getUsername()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword()))
                .thenReturn("encoded_password");
        when(walletService.createWallet(any(Parent.class)))
                .thenReturn(wallet);
        when(parentRepository.save(any(Parent.class)))
                .thenAnswer(invocation -> {
                    Parent parent = invocation.getArgument(0);
                    parent.setId(UUID.randomUUID());
                    return parent;
                });

        parentService.register(registerRequest);

        verify(parentRepository, times(1)).save(any(Parent.class));
    }

    @Test
    void shouldReturnSavedParentWhenRegistrationSuccessful() {

        UUID expectedId = UUID.randomUUID();

        when(parentRepository.findByUsername(registerRequest.getUsername()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword()))
                .thenReturn("encoded_password");
        when(walletService.createWallet(any(Parent.class)))
                .thenReturn(wallet);
        when(parentRepository.save(any(Parent.class)))
                .thenAnswer(invocation -> {
                    Parent parent = invocation.getArgument(0);
                    parent.setId(expectedId);
                    return parent;
                });

        Parent result = parentService.register(registerRequest);

        assertNotNull(result);
        assertEquals(expectedId, result.getId());
        assertEquals(registerRequest.getUsername(), result.getUsername());
        assertEquals(wallet, result.getWallet());
    }

    @Test
    void shouldCheckUsernameExistenceFirst() {

        when(parentRepository.findByUsername(registerRequest.getUsername()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword()))
                .thenReturn("encoded_password");
        when(walletService.createWallet(any(Parent.class)))
                .thenReturn(wallet);
        when(parentRepository.save(any(Parent.class)))
                .thenAnswer(invocation -> {
                    Parent parent = invocation.getArgument(0);
                    parent.setId(UUID.randomUUID());
                    return parent;
                });

        parentService.register(registerRequest);

        verify(parentRepository, times(1)).findByUsername(registerRequest.getUsername());

        InOrder inOrder = inOrder(parentRepository, passwordEncoder, walletService);

        inOrder.verify(parentRepository).findByUsername(registerRequest.getUsername());
        inOrder.verify(passwordEncoder).encode(anyString());
        inOrder.verify(walletService).createWallet(any(Parent.class));
        inOrder.verify(parentRepository).save(any(Parent.class));
    }
}

