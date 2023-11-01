package no.nav.pto.veilarbportefolje.arbeidsliste.v2;

import no.nav.common.types.identer.Fnr;

public record ArbeidslisteV2Request(
        Fnr fnr,
        String overskrift,
        String kommentar,
        String frist,
        String kategori
) {
}
