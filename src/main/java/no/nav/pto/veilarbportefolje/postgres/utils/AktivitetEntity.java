package no.nav.pto.veilarbportefolje.postgres.utils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Set;

import static java.util.Collections.emptySet;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class AktivitetEntity extends AktivitetStatusData {
    private Set<String> alleAktiviteter = emptySet();
}
