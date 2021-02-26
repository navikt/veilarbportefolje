package no.nav.pto.veilarbportefolje.pdldata;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static no.nav.pto.veilarbportefolje.database.Table.PDL_DATA.TABLE_NAME;
import static no.nav.pto.veilarbportefolje.util.TestUtil.setupInMemoryDatabase;
import static org.assertj.core.api.Assertions.assertThat;

public class PdlRepositoryTest {
    private PdlRepository pdlRepository ;
    private static AktorId AKTORID1 = AktorId.of("123456789");
    private static AktorId AKTORID2 = AktorId.of("1234567892");


    @Before
    public void setup() {
        JdbcTemplate jdbc = new JdbcTemplate(setupInMemoryDatabase());
        jdbc.execute("TRUNCATE TABLE "+ TABLE_NAME);
        this.pdlRepository = new PdlRepository(jdbc);
    }

    @Test
    public void skal_inserte_fodselsdag_pa_ulike_brukere() {
        LocalDate fodselsdag1 = DateUtils.getLocalDateFromSimpleISODate("1990-07-12");
        LocalDate fodselsdag2 = DateUtils.getLocalDateFromSimpleISODate("1994-02-08");

        pdlRepository.upsert(AKTORID1, fodselsdag1);
        pdlRepository.upsert(AKTORID2, fodselsdag2);

        assertThat(pdlRepository.hentFodselsdag(AKTORID1)).isEqualTo(fodselsdag1);
        assertThat(pdlRepository.hentFodselsdag(AKTORID2)).isEqualTo(fodselsdag2);

    }

    @Test
    public void skal_slette_pdl_data() {
        LocalDate fodselsdag = DateUtils.getLocalDateFromSimpleISODate("1990-07-12");

        pdlRepository.upsert(AKTORID1, fodselsdag);
        pdlRepository.slettPdlData(AKTORID1);

        assertThat(pdlRepository.hentFodselsdag(AKTORID1)).isNull();
    }

}
