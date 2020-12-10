package no.nav.pto.veilarbportefolje.sisteendring;

import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.List;

import static no.nav.pto.veilarbportefolje.TestUtil.setupInMemoryDatabase;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SisteEndringRepositoryTest {

    private SisteEndringRepository sisteEndringRepository;
    private static AktoerId AKTORID = AktoerId.of("123456789");

    @Before
    public void setup() {
        JdbcTemplate db = new JdbcTemplate(setupInMemoryDatabase());
        this.sisteEndringRepository = new SisteEndringRepository(db);
    }

    @Test
    public void mapDbTilOppfolgingsbruker() {
        String tidspunkt_1 = "2020-12-11T09:50:42.48+02:00";
        SisteEndringDTO dto_1 = new SisteEndringDTO()
                .setTidspunkt(ZonedDateTime.parse(tidspunkt_1))
                .setKategori(SisteEndringsKategorier.NY_IJOBB)
                .setAktoerId(AKTORID);

        String tidspunkt_2 = "2020-12-10T09:50:42.48+02:00";
        SisteEndringDTO dto_2 = new SisteEndringDTO()
                .setTidspunkt(ZonedDateTime.parse(tidspunkt_2))
                .setKategori(SisteEndringsKategorier.AVBRUTT_EGEN)
                .setAktoerId(AKTORID);

        sisteEndringRepository.upsert(dto_1);
        sisteEndringRepository.upsert(dto_2);

        OppfolgingsBruker bruker = new OppfolgingsBruker().setAktoer_id(AKTORID.getValue());
        sisteEndringRepository.setAlleSisteEndringTidspunkter(List.of(bruker));

        assertThat(bruker.getSiste_endring_ny_ijobb()).isEqualTo(tidspunkt_1);
        assertThat(bruker.getSiste_endring_avbrutt_egen()).isEqualTo(tidspunkt_2);
    }

}
