package com.immobilier.gestionImmobiliere.utils;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

public class DateUtils {

    private static final Locale LOCALE_FR = Locale.FRENCH;

    /**
     * Retourne le nom du mois en français, avec la première lettre en majuscule.
     * Ex: LocalDate.of(2026, 3, 15) -> "Mars"
     */
    public static String nomMoisFrancais(LocalDate date) {
        if (date == null) return null;
        String mois = date.getMonth().getDisplayName(TextStyle.FULL, LOCALE_FR);
        return mois.substring(0, 1).toUpperCase(LOCALE_FR) + mois.substring(1);
    }
}