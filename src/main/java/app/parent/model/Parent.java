package app.parent.model;

import app.child.model.Child;
import app.wallet.model.Wallet;
import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Parent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Child> children;

    @OneToOne(mappedBy = "owner", cascade = CascadeType.ALL)
    private Wallet wallet;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ParentRole role = ParentRole.ROLE_USER;

    private boolean isActive;

    private LocalDateTime createdOn;

    private LocalDateTime updatedOn;

    @PrePersist
    @PreUpdate
    protected void ensureRole() {
        if (this.role == null) {
            this.role = ParentRole.ROLE_USER;
        }
    }

    @PostLoad
    protected void ensureRoleAfterLoad() {
        if (this.role == null) {
            this.role = ParentRole.ROLE_USER;
        }
    }
}
