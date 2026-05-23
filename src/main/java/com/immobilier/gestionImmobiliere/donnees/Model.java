package com.immobilier.gestionImmobiliere.donnees;

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

        private Boolean isDeleted = false;

        private LocalDateTime createdAt;

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
