package com.immobilier.gestionImmobiliere.modules.statistiques.projection;

import java.math.BigDecimal;

public interface SumRetard {
    Long getNombre();
    BigDecimal getMontant();

    default long   nombreOrZero() { return getNombre()  == null ? 0L : getNombre(); }
    default BigDecimal montantOrZero(){ return getMontant() == null ? BigDecimal.valueOf(0d) : getMontant(); }
}