package no.nav.pto.veilarbportefolje.sisteendring;

import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static no.nav.pto.veilarbportefolje.TestUtil.setupInMemoryDatabase;
import static no.nav.pto.veilarbportefolje.util.DateUtils.iso8601FromTimestamp;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SisteEndringRepositoryTest {

    private SisteEndringRepository sisteEndringRepository;
    private static AktoerId AKTORID = AktoerId.of("123456789");

    @Before
    public void setup() {
        JdbcTemplate db = new JdbcTemplate(setupInMemoryDatabase());
        this.sisteEndringRepository = new SisteEndringRepository(db);
        sisteEndringRepository.slettSisteEndringer(AKTORID);
    }

    @Test
    public void mapDbTilOppfolgingsbruker() {
        ZonedDateTime zonedDateTime_1 = ZonedDateTime.now(ZoneId.of("Europe/Oslo"));
        String tidspunkt_1 = zonedDateTime_1.toOffsetDateTime().toString();
        SisteEndringDTO dto_1 = new SisteEndringDTO()
                .setTidspunkt(ZonedDateTime.parse(tidspunkt_1))
                .setKategori(SisteEndringsKategorier.NY_IJOBB)
                .setAktoerId(AKTORID);

        ZonedDateTime zonedDateTime_2 = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(3);
        String tidspunkt_2 = zonedDateTime_2.toOffsetDateTime().toString();
        SisteEndringDTO dto_2 = new SisteEndringDTO()
                .setTidspunkt(ZonedDateTime.parse(tidspunkt_2))
                .setKategori(SisteEndringsKategorier.AVBRUTT_EGEN)
                .setAktoerId(AKTORID);

        sisteEndringRepository.upsert(dto_1);
        sisteEndringRepository.upsert(dto_2);

        OppfolgingsBruker bruker = new OppfolgingsBruker().setAktoer_id(AKTORID.getValue());
        sisteEndringRepository.setAlleSisteEndringTidspunkter(List.of(bruker));

        assertThat(bruker.getSiste_endringer().getNy_ijobb()).isEqualTo(tidspunkt_1);
        assertThat(bruker.getSiste_endringer().getAvbrutt_egen()).isEqualTo(tidspunkt_2);
    }


    @Test
    public void slettSisteEndringer() {
        ZonedDateTime zonedDateTime_1 = ZonedDateTime.now(ZoneId.of("Europe/Oslo"));
        String tidspunkt_1 = zonedDateTime_1.toOffsetDateTime().toString();
        SisteEndringDTO dto_1 = new SisteEndringDTO()
                .setTidspunkt(ZonedDateTime.parse(tidspunkt_1))
                .setKategori(SisteEndringsKategorier.FULLFORT_STILLING)
                .setAktoerId(AKTORID);

        ZonedDateTime zonedDateTime_2 = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(3);
        String tidspunkt_2 = zonedDateTime_2.toOffsetDateTime().toString();
        SisteEndringDTO dto_2 = new SisteEndringDTO()
                .setTidspunkt(ZonedDateTime.parse(tidspunkt_2))
                .setKategori(SisteEndringsKategorier.MAL)
                .setAktoerId(AKTORID);

        sisteEndringRepository.upsert(dto_1);
        sisteEndringRepository.upsert(dto_2);
        Timestamp fraRepo_1 = sisteEndringRepository.getSisteEndringTidspunkt(AKTORID, SisteEndringsKategorier.FULLFORT_STILLING);
        Timestamp fraRepo_2 = sisteEndringRepository.getSisteEndringTidspunkt(AKTORID, SisteEndringsKategorier.MAL);

        assertThat(iso8601FromTimestamp(fraRepo_1)).isEqualTo(tidspunkt_1);
        assertThat(iso8601FromTimestamp(fraRepo_2)).isEqualTo(tidspunkt_2);

        sisteEndringRepository.slettSisteEndringer(AKTORID);

        Timestamp fraRepo_etter_sletting_1 = sisteEndringRepository.getSisteEndringTidspunkt(AKTORID, SisteEndringsKategorier.FULLFORT_STILLING);
        Timestamp fraRepo_etter_sletting_2 = sisteEndringRepository.getSisteEndringTidspunkt(AKTORID, SisteEndringsKategorier.MAL);

        assertThat(iso8601FromTimestamp(fraRepo_etter_sletting_1)).isNull();
        assertThat(iso8601FromTimestamp(fraRepo_etter_sletting_2)).isNull();
    }

}
