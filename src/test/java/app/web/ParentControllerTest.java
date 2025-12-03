package app.web;

import app.parent.model.Parent;
import app.parent.model.ParentRole;
import app.parent.repository.ParentRepository;
import app.parent.service.ParentService;
import app.security.UserData;
import app.web.dto.EditRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ParentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private ParentService parentService;

    private Parent parent;
    private UserData userData;
    private UUID parentId;

    @BeforeEach
    void setUp() {
        parentRepository.deleteAll();

        parent = Parent.builder()
                .username("testUser")
                .password("encodedPassword")
                .email("test@example.com")
                .firstName("Petar")
                .lastName("Ivanov")
                .role(ParentRole.ROLE_USER)
                .isActive(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        parent = parentRepository.save(parent);
        parentId = parent.getId();

        userData = new UserData(
                parentId,
                "testUser",
                "encodedPassword",
                true,
                ParentRole.ROLE_USER
        );
    }

    @Test
    void editProfile_shouldUpdateProfileSuccessfully() throws Exception {

        EditRequest editRequest = EditRequest.builder()
                .email("newemail@example.com")
                .password("newPassword123")
                .role(ParentRole.ROLE_USER)
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userData,
                userData.getPassword(),
                userData.getAuthorities()
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        
        mockMvc.perform(post("/home/profile")
                        .with(request -> {
                            request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
                            return request;
                        })
                        .param("email", editRequest.getEmail())
                        .param("password", editRequest.getPassword())
                        .param("role", editRequest.getRole().name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home/profile"))
                .andExpect(flash().attributeExists("successMessage"))
                .andExpect(flash().attribute("successMessage", "Profile updated successfully!"));

        Parent updatedParent = parentRepository.findById(parentId).orElseThrow();

        assertEquals("newemail@example.com", updatedParent.getEmail());
    }

    @Test
    void editProfile_shouldReturnErrorWhenValidationFails() throws Exception {

        String invalidEmail = "invalid-email";
        String shortPassword = "123";

        mockMvc.perform(post("/home/profile")
                        .with(request -> {
                            Authentication authentication = new UsernamePasswordAuthenticationToken(
                                    userData,
                                    userData.getPassword(),
                                    userData.getAuthorities()
                            );
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            return request;
                        })
                        .param("email", invalidEmail)
                        .param("password", shortPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home/profile"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attribute("errorMessage", "Please correct the highlighted fields."))
                .andExpect(flash().attributeExists("editRequest"))
                .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.editRequest"));
    }

    @Test
    void editProfile_shouldReturnErrorWhenEmailIsNull() throws Exception {

        mockMvc.perform(post("/home/profile")
                        .with(request -> {
                            Authentication authentication = new UsernamePasswordAuthenticationToken(
                                    userData,
                                    userData.getPassword(),
                                    userData.getAuthorities()
                            );
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            return request;
                        })
                        .param("password", "validPassword123")
                        .param("role", ParentRole.ROLE_USER.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home/profile"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attribute("errorMessage", "Please correct the highlighted fields."));
    }

    @Test
    void editProfile_shouldReturnErrorWhenPasswordIsTooShort() throws Exception {

        String shortPassword = "abc";

        mockMvc.perform(post("/home/profile")
                        .with(request -> {
                            Authentication authentication = new UsernamePasswordAuthenticationToken(
                                    userData,
                                    userData.getPassword(),
                                    userData.getAuthorities()
                            );
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            return request;
                        })
                        .param("email", "valid@example.com")
                        .param("password", shortPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home/profile"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attribute("errorMessage", "Please correct the highlighted fields."));
    }

    @Test
    void editProfile_shouldReturnErrorWhenPasswordIsTooLong() throws Exception {

        String longPassword = "a".repeat(17);

        mockMvc.perform(post("/home/profile")
                        .with(request -> {
                            Authentication authentication = new UsernamePasswordAuthenticationToken(
                                    userData,
                                    userData.getPassword(),
                                    userData.getAuthorities()
                            );
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            return request;
                        })
                        .param("email", "valid@example.com")
                        .param("password", longPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home/profile"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attribute("errorMessage", "Please correct the highlighted fields."));
    }

    @Test
    void editProfile_shouldReturnErrorWhenEmailIsInvalid() throws Exception {

        String invalidEmail = "not-an-email";

        mockMvc.perform(post("/home/profile")
                        .with(request -> {
                            Authentication authentication = new UsernamePasswordAuthenticationToken(
                                    userData,
                                    userData.getPassword(),
                                    userData.getAuthorities()
                            );
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            return request;
                        })
                        .param("email", invalidEmail)
                        .param("password", "validPassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home/profile"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attribute("errorMessage", "Please correct the highlighted fields."));
    }
}
