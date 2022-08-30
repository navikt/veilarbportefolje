package no.nav.pto.veilarbportefolje.postgres.utils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Set;

import static java.util.Collections.emptySet;
import static no.nav.pto.veilarbportefolje.util.DateUtils.getFarInTheFutureDate;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class AktivitetEntity extends AktivitetStatusData {
    private Set<String> alleAktiviteter = emptySet();
    private String nesteCvKanDelesStatus = null;
    private String nesteSvarfristStillingFraNav = getFarInTheFutureDate();
}
