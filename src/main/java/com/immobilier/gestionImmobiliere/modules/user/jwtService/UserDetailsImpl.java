package com.immobilier.gestionImmobiliere.modules.user.jwtService;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.immobilier.gestionImmobiliere.donnees.roles.model.Role;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class UserDetailsImpl implements UserDetails {

    private static final long serialVersionUID = 1L;

    // Getters spécifiques
    @Getter
    private Integer idUser;
    @Getter
    private String nom;
    @Getter
    private String prenom;
    @Getter
    private String email;
    @Getter
    private String telephone;

    @JsonIgnore
    private String password;

    @Getter
    private Role role;
    @Getter
    private Boolean actif;

    private Collection<? extends GrantedAuthority> authorities;

    // Constructeur complet
    public UserDetailsImpl(Integer idUser, String nom, String prenom, String email,
                           String telephone, String password, Role role, Boolean actif,
                           Collection<? extends GrantedAuthority> authorities) {
        this.idUser = idUser;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.password = password;
        this.role = role;
        this.actif = actif;
        this.authorities = authorities;
    }


    // Méthode build à partir de l'entité User
    public static UserDetailsImpl build(User user) {
        // Convertir le rôle en GrantedAuthority
        GrantedAuthority authority = new SimpleGrantedAuthority(
                user.getRole() != null ? user.getRole().getLibelleRole().toString() : "ROLE_USER"
        );

        Collection<GrantedAuthority> authorities = Collections.singletonList(authority);

        return new UserDetailsImpl(
                user.getIdUser(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                user.getTelephone(),
                user.getPassword(),
                user.getRole(),
                user.isActive(),
                authorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email; // Utilisation de l'email comme username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return actif != null && actif;
    }

    public String getFullName() {
        return (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(idUser, user.idUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUser);
    }

    @Override
    public String toString() {
        return "UserDetailsImpl{" +
                "idUser=" + idUser +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", actif=" + actif +
                ", role=" + (role != null ? role.getLibelleRole() : null) +
                '}';
    }}
