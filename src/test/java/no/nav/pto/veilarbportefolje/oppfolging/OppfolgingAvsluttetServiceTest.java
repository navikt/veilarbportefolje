package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static no.nav.pto.veilarbportefolje.util.TestDataUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

class OppfolgingAvsluttetServiceTest extends EndToEndTest {

    private final OppfolgingAvsluttetService oppfolgingAvsluttetService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public OppfolgingAvsluttetServiceTest(OppfolgingAvsluttetService oppfolgingAvsluttetService, JdbcTemplate jdbcTemplate) {
        this.oppfolgingAvsluttetService = oppfolgingAvsluttetService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void skal_slette_arbeidsliste_og_avslutte_oppfølging() {
        final AktoerId aktoerId = randomAktoerId();

        testDataClient.setupBrukerMedArbeidsliste(aktoerId, randomNavKontor(), randomVeilederId());

        String payload = new JSONObject()
                .put("aktoerId", aktoerId.toString())
                .put("sluttdato", "2020-12-01T00:00:00+02:00")
                .toString();

        oppfolgingAvsluttetService.behandleKafkaMelding(payload);

        String result = SqlUtils
                .select(jdbcTemplate, Table.ARBEIDSLISTE.TABLE_NAME, rs -> rs.getString(Table.ARBEIDSLISTE.AKTOERID))
                .column(Table.ARBEIDSLISTE.AKTOERID)
                .where(WhereClause.equals(Table.ARBEIDSLISTE.AKTOERID, aktoerId.getValue()))
                .execute();

        assertThat(result).isNull();

        assertThat(testDataClient.hentOppfolgingFlaggFraDatabase(aktoerId)).isEqualTo("N");

        final Map<String, Object> source = elasticTestClient.fetchDocument(aktoerId).getSourceAsMap();

        final boolean arbeidslisteAktiv = (boolean) source.get("arbeidsliste_aktiv");
        assertThat(arbeidslisteAktiv).isFalse();

        final boolean oppfolging = (boolean) source.get("oppfolging");
        assertThat(oppfolging).isFalse();
    }

}
