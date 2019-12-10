package no.nav.fo.veilarbportefolje.krr;

import no.nav.fo.veilarbportefolje.util.DbUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.util.List;

import static java.util.Collections.singletonList;
import static no.nav.fo.veilarbportefolje.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static no.nav.fo.veilarbportefolje.database.KrrTabell.KRR;
import static org.assertj.core.api.Assertions.assertThat;

public class KrrRepositoryTest {

    private KrrRepository krrRepository;
    private JdbcTemplate db;

    @Before
    public void setUp() {
        SingleConnectionDataSource ds = setupInMemoryDatabase();
        db = new JdbcTemplate(ds);
        krrRepository = new KrrRepository(db);
        krrRepository.slettKrrInformasjon();
    }

    @Test
    public void skal_lagre_og_slette_i_krr_tabell() {

        KrrKontaktInfoDTO dto = KrrKontaktInfoDTO.builder()
                .personident("00000000000")
                .reservert(true)
                .build();

        krrRepository.lagreKrrKontaktInfo(singletonList(dto));
        assertThat(antallRader()).isEqualTo(1);

        krrRepository.slettKrrInformasjon();
        assertThat(antallRader()).isEqualTo(0);
    }

    private int antallRader() {
        return DbUtils.selectCount(db, KRR).orElseThrow(RuntimeException::new);
    }
}
