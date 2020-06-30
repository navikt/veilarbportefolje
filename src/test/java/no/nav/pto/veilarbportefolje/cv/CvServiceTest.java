package no.nav.pto.veilarbportefolje.cv;

import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import static no.nav.common.utils.IdUtils.generateId;
import static no.nav.pto.veilarbportefolje.database.Table.BRUKER_DATA.*;
import static no.nav.pto.veilarbportefolje.util.DbUtils.boolToJaNei;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class CvServiceTest extends IntegrationTest {

    private static CvService cvService;

    @BeforeClass
    public static void beforeAll() {
        cvService = new CvService(
                new BrukerRepository(jdbcTemplate, null),
                new ElasticServiceV2(elasticClient)
        );
    }

    @Test
    public void skal_sette_har_delt_cv_til_nei_i_database() {
        final AktoerId aktoerId = insertBrukerMedDeltCv(true);
        final String harDeltCv = harDeltCvFraDatabase(aktoerId);

        assertThat(harDeltCv).isEqualTo("J");

        cvService.setHarDeltCvTilNei(aktoerId);

        String result = harDeltCvFraDatabase(aktoerId);
        assertThat(result).isEqualTo("N");
    }

    @Test
    public void skal_oppdatere_dokumentet_i_elastic() {
        final AktoerId aktoerId = insertBrukerMedDeltCv(false);
        Fnr fnr = Fnr.of("00000000000");

        String document = new JSONObject()
                .put("fnr", fnr.toString())
                .put("har_delt_cv", false)
                .toString();

        IndexResponse indexResponse = createDocument(fnr, document);
        assertThat(indexResponse.status().getStatus()).isEqualTo(201);

        String payload = new JSONObject()
                .put("aktoerId", aktoerId)
                .put("fnr", fnr)
                .put("meldingType", "SAMTYKKE_OPPRETTET")
                .put("ressurs", "CV_HJEMMEL")
                .toString();

        cvService.behandleKafkaMelding(payload);

        String harDeltCvDatabaseVerdi = harDeltCvFraDatabase(aktoerId);
        assertThat(harDeltCvDatabaseVerdi).isEqualTo("J");

        GetResponse getResponse = fetchDocument(fnr);
        assertThat(getResponse.isExists()).isTrue();

        boolean harDeltCv = (boolean) getResponse.getSourceAsMap().get("har_delt_cv");
        assertThat(harDeltCv).isTrue();
    }

    @Test
    public void skal_ikke_behandle_meldinger_som_har_meldingstype_arbeidsgiver_generell() {
        final AktoerId aktoerId = insertBrukerMedDeltCv(false);
        Fnr fnr = Fnr.of("00000000001");

        String document = new JSONObject()
                .put("fnr", fnr.toString())
                .put("har_delt_cv", false)
                .toString();

        IndexResponse indexResponse = createDocument(fnr, document);
        assertThat(indexResponse.status().getStatus()).isEqualTo(201);

        String payload = new JSONObject()
                .put("aktoerId", aktoerId)
                .put("fnr", fnr)
                .put("meldingType", "SAMTYKKE_OPPRETTET")
                .put("ressurs", "ARBEIDSGIVER_GENERELL")
                .toString();

        cvService.behandleKafkaMelding(payload);

        String harDeltCvDatabaseVerdi = harDeltCvFraDatabase(aktoerId);
        assertThat(harDeltCvDatabaseVerdi).isEqualTo("N");

        GetResponse getResponse = fetchDocument(fnr);
        assertThat(getResponse.isExists()).isTrue();

        boolean harDeltCv = (boolean) getResponse.getSourceAsMap().get("har_delt_cv");
        assertThat(harDeltCv).isFalse();
    }

    @Test
    public void skal_ikke_behandle_meldinger_som_har_meldingstype_cv_generell() {
        final AktoerId aktoerId = insertBrukerMedDeltCv(false);
        Fnr fnr = Fnr.of("00000000001");

        String document = new JSONObject()
                .put("fnr", fnr.toString())
                .put("har_delt_cv", false)
                .toString();

        IndexResponse indexResponse = createDocument(fnr, document);
        assertThat(indexResponse.status().getStatus()).isEqualTo(201);

        String payload = new JSONObject()
                .put("aktoerId", aktoerId)
                .put("fnr", fnr)
                .put("meldingType", "SAMTYKKE_OPPRETTET")
                .put("ressurs", "CV_GENERELL")
                .toString();

        cvService.behandleKafkaMelding(payload);

        String harDeltCvDatabaseVerdi = harDeltCvFraDatabase(aktoerId);
        assertThat(harDeltCvDatabaseVerdi).isEqualTo("N");

        GetResponse getResponse = fetchDocument(fnr);
        assertThat(getResponse.isExists()).isTrue();

        boolean harDeltCv = (boolean) getResponse.getSourceAsMap().get("har_delt_cv");
        assertThat(harDeltCv).isFalse();
    }

    @Test
    public void skal_ignorere_tilfeller_hvor_dokumentet_ikke_finnes_i_elastic() {
        final AktoerId aktoerId = insertBrukerMedDeltCv(false);
        Fnr fnr = Fnr.of("00000000000");

        String payload = new JSONObject()
                .put("aktoerId", aktoerId)
                .put("fnr", fnr)
                .put("meldingType", "SAMTYKKE_OPPRETTET")
                .put("ressurs", "CV_HJEMMEL")
                .toString();

        cvService.behandleKafkaMelding(payload);

        String harDeltCv = harDeltCvFraDatabase(aktoerId);
        assertThat(harDeltCv).isEqualTo("J");

        GetResponse getResponse = fetchDocument(fnr);
        assertThat(getResponse.isExists()).isFalse();
    }

    private static AktoerId insertBrukerMedDeltCv(boolean harDeltCv) {
        String aktoerId = generateId().substring(1, 11);
        SqlUtils
                .insert(jdbcTemplate, TABLE_NAME)
                .value(PERSONID, aktoerId)
                .value(AKTOERID, aktoerId)
                .value(HAR_DELT_CV, boolToJaNei(harDeltCv))
                .execute();

        return AktoerId.of(aktoerId);
    }

    private static String harDeltCvFraDatabase(AktoerId aktoerId) {
        return SqlUtils
                .select(jdbcTemplate, TABLE_NAME, rs -> rs.getString(HAR_DELT_CV))
                .column(HAR_DELT_CV)
                .where(WhereClause.equals(AKTOERID, aktoerId.toString()))
                .execute();
    }
}
