package no.nav.pto.veilarbportefolje.postgres.opensearch.utils;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetType;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class AktivitetEntity {
    AktivitetType aktivitetType;
    Timestamp utlop;
    Timestamp start;
}
