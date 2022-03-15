package no.nav.pto.veilarbportefolje.postgres.opensearch.utils;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

import static java.util.Collections.emptySet;

@Data
@Accessors(chain = true)
public class IkkeAvtaltAktivitetEntity extends AktivitetStatusData {
    private Set<String> alleAktiviteter = emptySet();
}
