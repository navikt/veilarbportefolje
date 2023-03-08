package no.nav.pto.veilarbportefolje.ensligforsorger.dto.input;

import java.util.List;

public record VedtakOvergangsstønadArbeidsoppfølging(
        Long vedtakId,
        String personIdent,
        List<Barn> barn,
        Stønadstype stønadstype,
        List<Periode> periode,
        Vedtaksresultat vedtaksresultat
) {
}


