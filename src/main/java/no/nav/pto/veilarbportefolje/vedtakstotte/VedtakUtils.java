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

     static Map<KafkaVedtakStatusEndring.Innsatsgruppe, Innsatsgruppe> mapInnsatsGruppeTilArenaInnsatsGruppe() {
        //TODO HVA SKA GRADERT VARIG INNSATS MAPPES TIL ?
        return new HashMap<KafkaVedtakStatusEndring.Innsatsgruppe,Innsatsgruppe>() {{
            put(KafkaVedtakStatusEndring.Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS, Innsatsgruppe.VARIG);
            put(KafkaVedtakStatusEndring.Innsatsgruppe.SITUASJONSBESTEMT_INNSATS, Innsatsgruppe.BFORM);
            put(KafkaVedtakStatusEndring.Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,   Innsatsgruppe.BATT);
            put(KafkaVedtakStatusEndring.Innsatsgruppe.STANDARD_INNSATS,   Innsatsgruppe.IKVAL);
            put(KafkaVedtakStatusEndring.Innsatsgruppe.VARIG_TILPASSET_INNSATS, Innsatsgruppe.VARIG);
        }};

    }

     static Map<KafkaVedtakStatusEndring.Hovedmal, Hovedmal> mapHovedMalTilArenaHovedmal() {
        return new HashMap<KafkaVedtakStatusEndring.Hovedmal, Hovedmal>() {{
            put( KafkaVedtakStatusEndring.Hovedmal.BEHOLDE_ARBEID, Hovedmal.BEHOLDEA);
            put(KafkaVedtakStatusEndring.Hovedmal.SKAFFE_ARBEID, Hovedmal.SKAFFEA);
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


    static Try<XContentBuilder> byggVedtakstotteJson(KafkaVedtakStatusEndring melding) {
        return Try.of(() ->
                jsonBuilder()
                        .startObject()
                        .field("vedtak_status", melding.getVedtakStatus().name())
                        .field("vedtak_status_endret", DateUtils.toIsoUTC(Timestamp.valueOf(melding.getStatusEndretTidspunkt())))
                        .endObject());
    }

}
