package no.nav.pto.veilarbportefolje;

import no.nav.pto.veilarbportefolje.domene.ArenaHovedmal;
import no.nav.pto.veilarbportefolje.domene.ArenaInnsatsgruppe;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;

public class PortefoljeMapper {
    public static ArenaInnsatsgruppe mapTilArenaInnsatsgruppe(Innsatsgruppe innsatsgruppe) {
        if (innsatsgruppe == null) {
            return null;
        }

        return switch (innsatsgruppe) {
            case STANDARD_INNSATS -> ArenaInnsatsgruppe.IKVAL;
            case SITUASJONSBESTEMT_INNSATS -> ArenaInnsatsgruppe.BFORM;
            case SPESIELT_TILPASSET_INNSATS -> ArenaInnsatsgruppe.BATT;
            case GRADERT_VARIG_TILPASSET_INNSATS, VARIG_TILPASSET_INNSATS -> ArenaInnsatsgruppe.VARIG;
        };
    }

    public static ArenaHovedmal mapTilArenaHovedmal(Hovedmal hovedmal) {
        if (hovedmal == null) {
            return null;
        }

        return switch (hovedmal) {
            case SKAFFE_ARBEID -> ArenaHovedmal.SKAFFEA;
            case BEHOLDE_ARBEID -> ArenaHovedmal.BEHOLDEA;
            case OKE_DELTAKELSE -> ArenaHovedmal.OKEDELT;
        };
    }
}
