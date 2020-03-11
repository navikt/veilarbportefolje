package no.nav.pto.veilarbportefolje.vedtakstotte;

import no.nav.pto.veilarbportefolje.domene.Innsatsgruppe;
import no.nav.pto.veilarbportefolje.domene.Hovedmal;

import java.util.HashMap;
import java.util.Map;

public class VedtakUtils {

    public static Map<KafkaVedtakStatusEndring.Innsatsgruppe, Innsatsgruppe> mapInnsatsGruppeTilArenaInnsatsGruppe() {
        //TODO HVA SKA GRADERT VARIG INNSATS MAPPES TIL ?
        return new HashMap<KafkaVedtakStatusEndring.Innsatsgruppe,Innsatsgruppe>() {{
            put(KafkaVedtakStatusEndring.Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS, Innsatsgruppe.VARIG);
            put(KafkaVedtakStatusEndring.Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, Innsatsgruppe.BFORM);
            put(KafkaVedtakStatusEndring.Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,   Innsatsgruppe.BATT);
            put(KafkaVedtakStatusEndring.Innsatsgruppe.STANDARD_INNSATS,   Innsatsgruppe.IKVAL);
            put(KafkaVedtakStatusEndring.Innsatsgruppe.VARIG_TILPASSET_INNSATS, Innsatsgruppe.VARIG);
        }};

    }

    public static Map<KafkaVedtakStatusEndring.Hovedmal, Hovedmal> mapHovedMalTilArenaHovedmal() {
        return new HashMap<KafkaVedtakStatusEndring.Hovedmal, Hovedmal>() {{
            put( KafkaVedtakStatusEndring.Hovedmal.BEHOLDE_ARBEID, Hovedmal.BEHOLDEA);
            put(KafkaVedtakStatusEndring.Hovedmal.SKAFFE_ARBEID, Hovedmal.SKAFFEA);
        }};
    }

}
