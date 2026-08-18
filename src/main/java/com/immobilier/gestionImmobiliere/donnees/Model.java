package com.immobilier.gestionImmobiliere.donnees;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import com.immobilier.gestionImmobiliere.utils.CustomDate;


@MappedSuperclass
    @Data
    @AllArgsConstructor
    public class Model {

        @Column(name = "is_deleted")
        private Boolean isDeleted = false;

        @Column(name = "created_at")
        private LocalDateTime createdAt;

        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

        public Model() {
            initTimestamp();
        }

        public void initTimestamp() {
            this.createdAt = CustomDate.now();
            this.updatedAt = CustomDate.now();
        }

    public static class ModelBuilder {
        public ModelBuilder() {}
    }


}
