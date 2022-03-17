package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.TestDataUtils;
import no.nav.sbl.sql.SqlUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class NyForVeilederServiceTest extends EndToEndTest {

    private final JdbcTemplate db;
    private final NyForVeilederService nyForVeilederService;
    private final OppfolgingRepositoryV2 oppfolgingRepository;

    @Autowired
    public NyForVeilederServiceTest(JdbcTemplate db, NyForVeilederService nyForVeilederService, OppfolgingRepositoryV2 oppfolgingRepository) {
        this.db = db;
        this.nyForVeilederService = nyForVeilederService;
        this.oppfolgingRepository = oppfolgingRepository;
    }

    @Test
    void skal_sette_ny_for_veileder_til_false_om_veileder_har_vært_inne_i_aktivitetsplan_til_bruker() {
        final AktorId aktoerId = TestDataUtils.randomAktorId();
        ;
        oppfolgingRepository.settUnderOppfolging(aktoerId, ZonedDateTime.now());
        oppfolgingRepository.settNyForVeileder(aktoerId, true);
        SqlUtils.insert(db, Table.OPPFOLGING_DATA.TABLE_NAME)
                .value(Table.OPPFOLGING_DATA.AKTOERID, aktoerId.get())
                .value(Table.OPPFOLGING_DATA.OPPFOLGING, "J")
                .value(Table.OPPFOLGING_DATA.NY_FOR_VEILEDER, "J")
                .execute();

        opensearchTestClient.createUserInOpensearch(aktoerId);

        NyForVeilederDTO melding = new NyForVeilederDTO(aktoerId, false);
        nyForVeilederService.behandleKafkaMeldingLogikk(melding);

        final Optional<BrukerOppdatertInformasjon> data = oppfolgingRepository.hentOppfolgingData(aktoerId);
        assertThat(data).isPresent();
        assertThat(data.get().getNyForVeileder()).isFalse();

        final boolean nyForVeileder = opensearchTestClient.hentBrukerFraOpensearch(aktoerId).isNy_for_veileder();
        assertThat(nyForVeileder).isFalse();
    }

    @Test
    void skal_ignorere_meldinger_hvor_ny_for_veileder_er_satt_til_true_siden_dette_gjøres_ved_tilordning() {
        final AktorId aktoerId = TestDataUtils.randomAktorId();
        oppfolgingRepository.settUnderOppfolging(aktoerId, ZonedDateTime.now());
        oppfolgingRepository.settNyForVeileder(aktoerId, false);

        SqlUtils.insert(db, Table.OPPFOLGING_DATA.TABLE_NAME)
                .value(Table.OPPFOLGING_DATA.AKTOERID, aktoerId.get())
                .value(Table.OPPFOLGING_DATA.OPPFOLGING, "J")
                .value(Table.OPPFOLGING_DATA.NY_FOR_VEILEDER, "N")
                .execute();

        opensearchTestClient.createUserInOpensearch(aktoerId);

        NyForVeilederDTO melding = new NyForVeilederDTO(aktoerId, true);

        nyForVeilederService.behandleKafkaMeldingLogikk(melding);

        final Optional<BrukerOppdatertInformasjon> data = oppfolgingRepository.hentOppfolgingData(aktoerId);
        assertThat(data).isPresent();
        //assertThat(data.get().getNyForVeileder()).isFalse();

        final boolean nyForVeileder = opensearchTestClient.hentBrukerFraOpensearch(aktoerId).isNy_for_veileder();
        //assertThat(nyForVeileder).isFalse();
    }
}
