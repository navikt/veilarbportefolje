package no.nav.pto.veilarbportefolje.cv;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class CVRepositoryV2Test {

    private JdbcTemplate db;
    private CVRepositoryV2 cvRepositoryV2;

    @Before
    public void setup() {
        db = SingletonPostgresContainer.init().createJdbcTemplate();
        cvRepositoryV2 = new CVRepositoryV2(db);
    }

    @Test
    public void skal_upserte_database() {
        AktorId aktoerId = AktorId.of("0");

        cvRepositoryV2.upsert(aktoerId, true);
        assertThat(cvRepositoryV2.harDeltCv(aktoerId).get()).isTrue();

        cvRepositoryV2.upsert(aktoerId, false);
        assertThat(cvRepositoryV2.harDeltCv(aktoerId).get()).isFalse();
    }

    @Test
    public void skal_slette_CV_for_aktoer() {
        AktorId aktoerId = AktorId.of("1");
        cvRepositoryV2.upsert(aktoerId, true);
        assertThat(cvRepositoryV2.harDeltCv(aktoerId).get()).isTrue();

        cvRepositoryV2.slettCVData(aktoerId);
        assertThat(cvRepositoryV2.harDeltCv(aktoerId).isEmpty());
    }
}
