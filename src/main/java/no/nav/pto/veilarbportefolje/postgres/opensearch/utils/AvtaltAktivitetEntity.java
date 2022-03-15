package no.nav.pto.veilarbportefolje.postgres.opensearch.utils;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

import static java.util.Collections.emptySet;

@Data
@Accessors(chain = true)
public class AvtaltAktivitetEntity extends AktivitetStatusData {
    private String aktoerId;

    private String nyesteUtlopteAktivitet;
    private String aktivitetStart;
    private String nesteAktivitetStart;
    private String forrigeAktivitetStart;

    private Set<String> aktiviteter = emptySet();
    private Set<String> tiltak = emptySet();
}
