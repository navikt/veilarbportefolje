package no.nav.pto.veilarbportefolje.postgres.opensearch;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

import static java.util.Collections.emptySet;
import static no.nav.pto.veilarbportefolje.util.DateUtils.getFarInTheFutureDate;

@Data
@Accessors(chain = true)
public class PostgresAktivitetEntity {
    private String aktoerId;

    private String nyesteUtlopteAktivitet;
    private String aktivitetStart;
    private String nesteAktivitetStart;
    private String forrigeAktivitetStart;
    private String aktivitetMoteUtlopsdato = getFarInTheFutureDate();
    private String aktivitetMoteStartdato;
    private String aktivitetStillingUtlopsdato = getFarInTheFutureDate();
    private String aktivitetEgenUtlopsdato = getFarInTheFutureDate();
    private String aktivitetUbehandlingUtlopsdato = getFarInTheFutureDate();
    private String aktivitetIjobbUtlopsdato = getFarInTheFutureDate();
    private String aktivitetSokeavtaleUtlopsdato = getFarInTheFutureDate();
    private String aktivitetTiltakUtlopsdato = getFarInTheFutureDate();
    private String aktivitetUtdanningaktivitetUtlopsdato = getFarInTheFutureDate();
    private String aktivitetGruppeaktivitetUtlopsdato = getFarInTheFutureDate();

    private Set<String> aktiviteter = emptySet();
    private Set<String> tiltak = emptySet(); // V
}
