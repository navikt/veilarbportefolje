package no.nav.pto.veilarbportefolje.cv;

import no.nav.pto.veilarbportefolje.TestUtil;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static no.nav.pto.veilarbportefolje.database.Table.BRUKER_CV.TABLE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class CvRepositoryTest {

    private static CvRepository cvRepository;
    private static JdbcTemplate jdbcTemplate;

    @BeforeClass
    public static void beforeClass() {
        SingleConnectionDataSource ds = TestUtil.setupInMemoryDatabase();
        jdbcTemplate = new JdbcTemplate(ds);
        cvRepository = new CvRepository(jdbcTemplate);
    }

    @After
    public void tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE " + TABLE_NAME);
    }

    @Test
    public void skal_upserte_database() {
        AktoerId aktoerId = AktoerId.of("0");
        Fnr fnr = Fnr.of("00000000000");

        cvRepository.upsert(aktoerId, fnr, true);
        assertThat(cvRepository.harDeltCv(aktoerId)).isEqualTo("J");

        cvRepository.upsert(aktoerId, fnr, false);
        assertThat(cvRepository.harDeltCv(aktoerId)).isEqualTo("N");
    }

}