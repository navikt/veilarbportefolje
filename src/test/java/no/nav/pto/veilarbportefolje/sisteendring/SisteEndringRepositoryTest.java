package no.nav.pto.veilarbportefolje.sisteendring;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori.*;
import static no.nav.pto.veilarbportefolje.util.TestUtil.setupInMemoryDatabase;
import static no.nav.pto.veilarbportefolje.util.DateUtils.toIsoUTC;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SisteEndringRepositoryTest {

    private SisteEndringRepository sisteEndringRepository;
    private static AktorId AKTORID = AktorId.of("123456789");

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
                .setKategori(NY_IJOBB)
                .setAktoerId(AKTORID)
                .setAktivtetId("1");

        ZonedDateTime zonedDateTime_2 = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(3);
        String tidspunkt_2 = zonedDateTime_2.toOffsetDateTime().toString();
        SisteEndringDTO dto_2 = new SisteEndringDTO()
                .setTidspunkt(ZonedDateTime.parse(tidspunkt_2))
                .setKategori(AVBRUTT_EGEN)
                .setAktoerId(AKTORID)
                .setAktivtetId("2");

        sisteEndringRepository.upsert(dto_1);
        sisteEndringRepository.upsert(dto_2);

        OppfolgingsBruker bruker = new OppfolgingsBruker().setAktoer_id(AKTORID.get());
        sisteEndringRepository.setAlleSisteEndringTidspunkter(List.of(bruker));

        assertThat(bruker.getSiste_endringer().get(NY_IJOBB.name()).getTidspunkt()).isEqualTo(toIsoUTC(ZonedDateTime.parse(tidspunkt_1)));
        assertThat(bruker.getSiste_endringer().get(NY_IJOBB.name()).getAktivtetId()).isEqualTo(dto_1.getAktivtetId());
        assertThat(bruker.getSiste_endringer().get(AVBRUTT_EGEN.name()).getTidspunkt()).isEqualTo(toIsoUTC(ZonedDateTime.parse(tidspunkt_2)));
        assertThat(bruker.getSiste_endringer().get(AVBRUTT_EGEN.name()).getAktivtetId()).isEqualTo(dto_2.getAktivtetId());
    }


    @Test
    public void slettSisteEndringer() {
        ZonedDateTime zonedDateTime_1 = ZonedDateTime.now(ZoneId.of("Europe/Oslo"));
        String tidspunkt_1 = zonedDateTime_1.toOffsetDateTime().toString();
        SisteEndringDTO dto_1 = new SisteEndringDTO()
                .setTidspunkt(ZonedDateTime.parse(tidspunkt_1))
                .setKategori(SisteEndringsKategori.FULLFORT_STILLING)
                .setAktoerId(AKTORID);

        ZonedDateTime zonedDateTime_2 = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusDays(3);
        String tidspunkt_2 = zonedDateTime_2.toOffsetDateTime().toString();
        SisteEndringDTO dto_2 = new SisteEndringDTO()
                .setTidspunkt(ZonedDateTime.parse(tidspunkt_2))
                .setKategori(SisteEndringsKategori.MAL)
                .setAktoerId(AKTORID);

        sisteEndringRepository.upsert(dto_1);
        sisteEndringRepository.upsert(dto_2);
        Timestamp fraRepo_1 = sisteEndringRepository.getSisteEndringTidspunkt(AKTORID, SisteEndringsKategori.FULLFORT_STILLING);
        Timestamp fraRepo_2 = sisteEndringRepository.getSisteEndringTidspunkt(AKTORID, SisteEndringsKategori.MAL);

        assertThat(toIsoUTC(fraRepo_1)).isEqualTo(toIsoUTC(ZonedDateTime.parse(tidspunkt_1)));
        assertThat(toIsoUTC(fraRepo_2)).isEqualTo(toIsoUTC(ZonedDateTime.parse(tidspunkt_2)));

        sisteEndringRepository.slettSisteEndringer(AKTORID);

        Timestamp fraRepo_etter_sletting_1 = sisteEndringRepository.getSisteEndringTidspunkt(AKTORID, SisteEndringsKategori.FULLFORT_STILLING);
        Timestamp fraRepo_etter_sletting_2 = sisteEndringRepository.getSisteEndringTidspunkt(AKTORID, SisteEndringsKategori.MAL);

        assertThat(toIsoUTC(fraRepo_etter_sletting_1)).isNull();
        assertThat(toIsoUTC(fraRepo_etter_sletting_2)).isNull();
    }

}
