package com.immobilier.gestionImmobiliere.donnees;

import com.immobilier.gestionImmobiliere.utils.CustomDate;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;



@MappedSuperclass
@Data
@AllArgsConstructor
public class Model_1 {

        private Boolean isDeleted = false;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;

        public Model_1() {
            initTimestamp();
        }

        public void initTimestamp() {
            this.createdAt = CustomDate.now();
            this.updatedAt = CustomDate.now();
        }

        public static class Model_1Builder {
            public Model_1Builder() {}
        }

}
