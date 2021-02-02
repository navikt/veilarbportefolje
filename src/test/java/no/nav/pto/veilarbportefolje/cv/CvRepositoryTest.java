package no.nav.pto.veilarbportefolje.cv;

import no.nav.pto.veilarbportefolje.util.TestUtil;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
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

        cvRepository.upsert(aktoerId, true);
        assertThat(cvRepository.harDeltCv(aktoerId)).isEqualTo("J");

        cvRepository.upsert(aktoerId, false);
        assertThat(cvRepository.harDeltCv(aktoerId)).isEqualTo("N");
    }

}
