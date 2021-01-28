package no.nav.pto.veilarbportefolje.kafka;

import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.pto.veilarbportefolje.util.ElasticTestClient;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.sbl.sql.SqlUtils;
import org.elasticsearch.action.get.GetResponse;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.DateUtils.*;
import static no.nav.pto.veilarbportefolje.util.ElasticTestClient.pollElasticUntil;
import static org.assertj.core.api.Assertions.assertThat;

class AktivitetKafkaConsumerTest extends EndToEndTest {

    private final JdbcTemplate db;
    private final ElasticTestClient elasticTestClient;
    private final AktivitetService aktivitetService;

    @Autowired
    public AktivitetKafkaConsumerTest(JdbcTemplate db, ElasticTestClient elasticTestClient, AktivitetService aktivitetService) {
        this.db = db;
        this.elasticTestClient = elasticTestClient;
        this.aktivitetService = aktivitetService;
    }

    @Test
    void skal_oppdatere_aktivitet_i_elastic() {
        final AktorId aktoerId = AktorId.of("123456789");
        final PersonId personId = PersonId.of("1234");
        final Fnr fnr = Fnr.of("00000000000");

        final String tilDato = (LocalDate.now().plusMonths(1)).atStartOfDay().atZone(ZoneId.of("Europe/Oslo")).toInstant().toString();

        SqlUtils.insert(db, Table.OPPFOLGINGSBRUKER.TABLE_NAME)
                .value(Table.OPPFOLGINGSBRUKER.PERSON_ID, personId.toString())
                .value(Table.OPPFOLGINGSBRUKER.FODSELSNR, fnr.toString())
                .execute();

        SqlUtils.insert(db, Table.AKTOERID_TO_PERSONID.TABLE_NAME)
                .value(Table.AKTOERID_TO_PERSONID.AKTOERID, aktoerId.toString())
                .value(Table.AKTOERID_TO_PERSONID.PERSONID, personId.toString())
                .value(Table.AKTOERID_TO_PERSONID.GJELDENE, true)
                .execute();

        SqlUtils.insert(db, Table.OPPFOLGING_DATA.TABLE_NAME)
                .value(Table.OPPFOLGING_DATA.AKTOERID, aktoerId.toString())
                .value(Table.OPPFOLGING_DATA.OPPFOLGING, "J")
                .execute();

        createAktivitetDocument(aktoerId);

        String melding = new JSONObject()
                .put("aktivitetId", 1)
                .put("aktorId", aktoerId.toString())
                .put("fraDato", "2020-08-31T10:03:20+02:00")
                .put("tilDato", tilDato)
                .put("endretDato", "2020-07-29T15:43:41.049+02:00")
                .put("aktivitetType", "IJOBB")
                .put("aktivitetStatus", "GJENNOMFORES")
                .put("avtalt", true)
                .put("historisk", false)
                .put("version", 1)
                .toString();

        aktivitetService.behandleKafkaMelding(melding);

        pollElasticUntil(() -> aktivitetIJobbUtlopsdatoErOppdatert(aktoerId));

        final String aktivitetIJobbUtlopsdato = getAktivitetIJobbUtlopsdato(elasticTestClient.fetchDocument(aktoerId));

        assertThat(aktivitetIJobbUtlopsdato).isEqualTo(toIsoUTC(timestampFromISO8601(tilDato)));
    }

    private Boolean aktivitetIJobbUtlopsdatoErOppdatert(AktorId aktoerId) {
        return !Optional.of(elasticTestClient.fetchDocument(aktoerId))
                .map(AktivitetKafkaConsumerTest::getAktivitetIJobbUtlopsdato)
                .map(utlopsDato -> utlopsDato.equals(getFarInTheFutureDate()))
                .get();
    }

    private static String getAktivitetIJobbUtlopsdato(GetResponse get1) {
        return (String) get1.getSourceAsMap().get("aktivitet_ijobb_utlopsdato");
    }


    private void createAktivitetDocument(AktorId aktoerId) {
        String document = new JSONObject()
                .put("aktivitet_mote_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_stilling_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_egen_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_behandling_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_ijobb_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_sokeavtale_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_tiltak_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_utdanningaktivitet_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktivitet_gruppeaktivitet_utlopsdato", DateUtils.getFarInTheFutureDate())
                .put("aktoer_id", aktoerId.toString())
                .toString();

        elasticTestClient.createDocument(aktoerId, document);
    }
}
