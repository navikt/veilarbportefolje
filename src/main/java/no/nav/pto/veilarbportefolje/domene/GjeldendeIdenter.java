package no.nav.pto.veilarbportefolje.domene;

import lombok.Builder;
import lombok.Value;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.opensearch.domene.OppfolgingsBruker;

@Value
@Builder
public class GjeldendeIdenter {
    Fnr fnr;
    AktorId aktorId;

    public static GjeldendeIdenter of(Bruker bruker) {
        return GjeldendeIdenter.builder().aktorId(AktorId.of(bruker.getAktoerid())).fnr(Fnr.of(bruker.getFnr())).build();
    }

    public static GjeldendeIdenter of(OppfolgingsBruker oppfolgingsBruker) {
        return GjeldendeIdenter.builder().aktorId(AktorId.of(oppfolgingsBruker.getAktoer_id())).fnr(Fnr.of(oppfolgingsBruker.getFnr())).build();
    }
}
