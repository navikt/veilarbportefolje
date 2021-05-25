package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.apache.tomcat.jni.Time;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class OppfolgingRepositoryTestV2 {

    private JdbcTemplate db;
    private OppfolgingRepositoryV2 oppfolgingRepository;
    private final AktorId aktoerId = AktorId.of("0");

    @Before
    public void setup() {
        db = SingletonPostgresContainer.init().createJdbcTemplate();
        oppfolgingRepository = new OppfolgingRepositoryV2(db);
        oppfolgingRepository.slettOppfolgingData(aktoerId);
    }

    @Test
    public void skal_sette_oppfolging_til_false() {
        oppfolgingRepository.settUnderOppfolging(aktoerId, ZonedDateTime.now());
        oppfolgingRepository.settOppfolgingTilFalse(aktoerId);

        boolean oppfolging = SqlUtils.select(db, Table.OPPFOLGING_DATA.TABLE_NAME, rs -> rs.getBoolean(Table.OPPFOLGING_DATA.OPPFOLGING))
                .column(Table.OPPFOLGING_DATA.OPPFOLGING)
                .where(WhereClause.equals(Table.OPPFOLGING_DATA.AKTOERID, aktoerId.toString()))
                .execute();

        assertThat(oppfolging).isEqualTo(false);
    }

    @Test
    public void skal_sette_ny_veileder() {
        VeilederId veilederId = VeilederId.of("Z12345");
        oppfolgingRepository.settUnderOppfolging(aktoerId, ZonedDateTime.now());
        oppfolgingRepository.settVeileder(aktoerId, veilederId);

        BrukerOppdatertInformasjon brukerOppdatertInformasjon = oppfolgingRepository.hentOppfolgingData(aktoerId).get();
        assertThat(VeilederId.of(brukerOppdatertInformasjon.getVeileder())).isEqualTo(veilederId);
    }

}
