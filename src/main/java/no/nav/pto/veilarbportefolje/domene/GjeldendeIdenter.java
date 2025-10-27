package no.nav.pto.veilarbportefolje.domene;

import lombok.Builder;
import lombok.Value;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell;

@Value
@Builder
public class GjeldendeIdenter {
    Fnr fnr;
    AktorId aktorId;

    public static GjeldendeIdenter of(PortefoljebrukerFrontendModell brukerFrontendModell) {
        return GjeldendeIdenter.builder().aktorId(AktorId.of(brukerFrontendModell.getAktoerid())).fnr(Fnr.of(brukerFrontendModell.getFnr())).build();
    }

    public static GjeldendeIdenter of(PortefoljebrukerOpensearchModell brukerOpensearchModell) {
        return GjeldendeIdenter.builder().aktorId(AktorId.of(brukerOpensearchModell.getAktoer_id())).fnr(Fnr.of(brukerOpensearchModell.getFnr())).build();
    }
}
