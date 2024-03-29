package no.nav.pto.veilarbportefolje.postgres.utils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Set;

import static java.util.Collections.emptySet;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class AvtaltAktivitetEntity extends AktivitetStatusData {
    private String nyesteUtlopteAktivitet;
    private String aktivitetStart;
    private String nesteAktivitetStart;
    private String forrigeAktivitetStart;

    private Set<String> aktiviteter = emptySet();
    private Set<String> tiltak = emptySet();
}
