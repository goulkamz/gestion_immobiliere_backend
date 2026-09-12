package com.immobilier.gestionImmobiliere.modules.statistiques.projection;

import java.math.BigDecimal;

public interface SumDuPaye {
    BigDecimal getMontantDu();
    BigDecimal getMontantPaye();

    default BigDecimal montantDuOrZero()   { return getMontantDu()   == null ? BigDecimal.valueOf(0d) : getMontantDu(); }
    default BigDecimal montantPayeOrZero() { return getMontantPaye() == null ? BigDecimal.valueOf(0d) : getMontantPaye(); }
}
