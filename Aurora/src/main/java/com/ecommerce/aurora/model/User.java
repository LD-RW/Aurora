package com.ecommerce.aurora.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "users",
        uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
        })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;


    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    @NotBlank
    @Size(max = 20)
    @Column(name = "username")
    private String username;
    @NotBlank
    @Size(max = 120)
    @Column(name = "password")
    private String password;
    @NotBlank
    @Size(max = 50)
    @Email
    @Column(name = "email")
    private String email;


    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE} ,fetch = FetchType.LAZY)
    @JoinTable(name = "user_role",
                joinColumns = @JoinColumn(name = "user_id"),
                inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles =  new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE} ,fetch = FetchType.LAZY)
    @JoinTable(name = "user_address",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "address_id"))
            private List<Address> addresses = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE},
                orphanRemoval = true,fetch = FetchType.LAZY)
    private Set<Product> products = new HashSet<>();
}
