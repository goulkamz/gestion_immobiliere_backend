package com.immobilier.gestionImmobiliere.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public class CustomDate {

    public static LocalDateTime now() {
        return ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
    }

}
