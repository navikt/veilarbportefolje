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
                        .field("vedtak_status", melding.getVedtakStatusEndring().name())
                        .field("vedtak_status_endret", DateUtils.toIsoUTC(Timestamp.valueOf(melding.getTimestamp())))
                        .endObject());
    }

}
