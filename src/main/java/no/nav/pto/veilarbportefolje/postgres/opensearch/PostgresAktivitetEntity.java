package no.nav.pto.veilarbportefolje.postgres.opensearch;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

import static java.util.Collections.emptySet;

@Data
@Accessors(chain = true)
public class PostgresAktivitetEntity {
    private String aktoerId;

    private String nyesteUtlopteAktivitet;
    private String aktivitetStart;
    private String nesteAktivitetStart;
    private String forrigeAktivitetStart;
    private String aktivitetMoteUtlopsdato;
    private String aktivitetMoteStartdato;
    private String aktivitetStillingUtlopsdato;
    private String aktivitetEgenUtlopsdato;
    private String aktivitetUbehandlingUtlopsdato;
    private String aktivitetIjobbUtlopsdato;
    private String aktivitetSokeavtaleUtlopsdato;
    private String aktivitetTiltakUtlopsdato;
    private String aktivitetUtdanningaktivitetUtlopsdato;
    private String aktivitetGruppeaktivitetUtlopsdato;

    private Set<String> aktiviteter = emptySet();
    private Set<String> tiltak = emptySet();
}
