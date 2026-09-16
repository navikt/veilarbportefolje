package no.nav.pto.veilarbportefolje.ensligforsorger.dto.input;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.Stønadstype;
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.Vedtaksresultat;

import java.util.List;

public record VedtakOvergangsstønadArbeidsoppfølging(
        Long vedtakId,
        String personIdent,
        List<Barn> barn,
        Stønadstype stønadstype,
        List<Periode> periode,
        Vedtaksresultat vedtaksresultat
) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public VedtakOvergangsstønadArbeidsoppfølging {
    }

}


