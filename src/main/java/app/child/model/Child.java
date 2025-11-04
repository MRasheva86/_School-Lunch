package app.child.model;


import app.parent.model.Parent;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "child")
public class Child {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "parent_id")
	private Parent parent;

    @Column(nullable = false)
    private String school;

    @Column(nullable = false)
    private int grade;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ChildGender gender;
}
