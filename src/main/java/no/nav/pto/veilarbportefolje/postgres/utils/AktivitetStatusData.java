package no.nav.pto.veilarbportefolje.postgres.utils;

import lombok.Data;
import lombok.experimental.Accessors;

import static no.nav.pto.veilarbportefolje.util.DateUtils.getFarInTheFutureDate;

@Data
@Accessors(chain = true)
public abstract class AktivitetStatusData {
    private String aktivitetMoteStartdato = getFarInTheFutureDate();
    private String aktivitetMoteUtlopsdato = getFarInTheFutureDate();
    private String aktivitetStillingUtlopsdato = getFarInTheFutureDate();
    private String aktivitetEgenUtlopsdato = getFarInTheFutureDate();
    private String aktivitetBehandlingUtlopsdato = getFarInTheFutureDate();
    private String aktivitetIjobbUtlopsdato = getFarInTheFutureDate();
    private String aktivitetSokeavtaleUtlopsdato = getFarInTheFutureDate();
    private String aktivitetTiltakUtlopsdato = getFarInTheFutureDate();
    private String aktivitetUtdanningaktivitetUtlopsdato = getFarInTheFutureDate();
    private String aktivitetGruppeaktivitetUtlopsdato = getFarInTheFutureDate();
}
