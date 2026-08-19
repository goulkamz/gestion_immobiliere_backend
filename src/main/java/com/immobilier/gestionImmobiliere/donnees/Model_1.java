package com.immobilier.gestionImmobiliere.donnees;

import com.immobilier.gestionImmobiliere.utils.CustomDate;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;



@MappedSuperclass
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Model_1 {

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @PrePersist
    public void initTimestamp() {
        if (this.createdAt == null) this.createdAt = CustomDate.now();
        this.updatedAt = CustomDate.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = CustomDate.now();
    }

}
