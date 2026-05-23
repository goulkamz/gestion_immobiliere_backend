package com.immobilier.gestionImmobiliere.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class CustomDate {

    public static LocalDateTime now() {
        return ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
    }

}
