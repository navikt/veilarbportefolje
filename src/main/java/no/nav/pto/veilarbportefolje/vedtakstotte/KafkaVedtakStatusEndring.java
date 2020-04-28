package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class KafkaVedtakStatusEndring {

    public enum KafkaVedtakStatus {
        UTKAST_OPPRETTET, SENDT_TIL_BESLUTTER, SENDT_TIL_BRUKER, UTKAST_SLETTET
    }

    public enum Hovedmal {
        SKAFFE_ARBEID, BEHOLDE_ARBEID
    }

    public enum Innsatsgruppe {
        STANDARD_INNSATS,
        SITUASJONSBESTEMT_INNSATS,
        SPESIELT_TILPASSET_INNSATS,
        GRADERT_VARIG_TILPASSET_INNSATS,
        VARIG_TILPASSET_INNSATS
    }

    long vedtakId;
    String aktorId;
    KafkaVedtakStatus vedtakStatus;
    Innsatsgruppe innsatsgruppe;
    Hovedmal hovedmal;
    LocalDateTime statusEndretTidspunkt;

    public static Map<Innsatsgruppe, no.nav.pto.veilarbportefolje.domene.Innsatsgruppe> mapInnsatsGruppeTilArenaInnsatsGruppe() {
        //TODO HVA SKA GRADERT VARIG INNSATS MAPPES TIL ?
        return new HashMap<Innsatsgruppe,no.nav.pto.veilarbportefolje.domene.Innsatsgruppe>() {{
            put( Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS, no.nav.pto.veilarbportefolje.domene.Innsatsgruppe.VARIG);
            put(Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, no.nav.pto.veilarbportefolje.domene.Innsatsgruppe.BFORM);
            put(Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,    no.nav.pto.veilarbportefolje.domene.Innsatsgruppe.BATT);
            put(Innsatsgruppe.STANDARD_INNSATS,    no.nav.pto.veilarbportefolje.domene.Innsatsgruppe.IKVAL);
            put(Innsatsgruppe.VARIG_TILPASSET_INNSATS,    no.nav.pto.veilarbportefolje.domene.Innsatsgruppe.VARIG);
        }};

    }

    public static Map<Hovedmal, no.nav.pto.veilarbportefolje.domene.Hovedmal> mapHovedMalTilArenaHovedmal() {
        return new HashMap<Hovedmal,no.nav.pto.veilarbportefolje.domene.Hovedmal>() {{
            put( Hovedmal.BEHOLDE_ARBEID, no.nav.pto.veilarbportefolje.domene.Hovedmal.BEHOLDEA);
            put(Hovedmal.SKAFFE_ARBEID, no.nav.pto.veilarbportefolje.domene.Hovedmal.SKAFFEA);
        }};

    }
}
