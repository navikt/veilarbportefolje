package no.nav.pto.veilarbportefolje.vedtakstotte;

import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.domene.Innsatsgruppe;
import no.nav.pto.veilarbportefolje.domene.Hovedmal;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class VedtakUtils {

     static Map<VedtakStatusEndring.Innsatsgruppe, Innsatsgruppe> mapInnsatsGruppeTilArenaInnsatsGruppe() {
        //TODO HVA SKA GRADERT VARIG INNSATS MAPPES TIL ?
        return new HashMap<VedtakStatusEndring.Innsatsgruppe,Innsatsgruppe>() {{
            put(VedtakStatusEndring.Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS, Innsatsgruppe.VARIG);
            put(VedtakStatusEndring.Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, Innsatsgruppe.BFORM);
            put(VedtakStatusEndring.Innsatsgruppe.SPESIELT_TILPASSET_INNSATS, Innsatsgruppe.BATT);
            put(VedtakStatusEndring.Innsatsgruppe.STANDARD_INNSATS, Innsatsgruppe.IKVAL);
            put(VedtakStatusEndring.Innsatsgruppe.VARIG_TILPASSET_INNSATS, Innsatsgruppe.VARIG);
        }};

    }

     static Map<VedtakStatusEndring.Hovedmal, Hovedmal> mapHovedMalTilArenaHovedmal() {
        return new HashMap<VedtakStatusEndring.Hovedmal, Hovedmal>() {{
            put(VedtakStatusEndring.Hovedmal.BEHOLDE_ARBEID, Hovedmal.BEHOLDEA);
            put(VedtakStatusEndring.Hovedmal.SKAFFE_ARBEID, Hovedmal.SKAFFEA);
        }};
    }

     static Try<XContentBuilder> byggVedtakstotteNullVerdiJson() {
        return Try.of(() ->
                jsonBuilder()
                        .startObject()
                        .nullField("vedtak_status")
                        .nullField("vedtak_status_endret")
                        .endObject());
    }


    static Try<XContentBuilder> byggVedtakstotteJson(VedtakStatusEndring melding) {
        return Try.of(() ->
                jsonBuilder()
                        .startObject()
                        .field("vedtak_status", melding.getVedtakStatus().name())
                        .field("vedtak_status_endret", DateUtils.toIsoUTC(Timestamp.valueOf(melding.getStatusEndretTidspunkt())))
                        .endObject());
    }

}
