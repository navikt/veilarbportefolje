package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.TestDataUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OppfolgingStartetServiceTest extends EndToEndTest {

    private final OppfolgingStartetService oppfolgingStartetService;
    private final OppfolgingRepository oppfolgingRepository;

    @Autowired
    public OppfolgingStartetServiceTest(OppfolgingRepository oppfolgingRepository, JdbcTemplate db) {
        this.oppfolgingRepository = oppfolgingRepository;
        BrukerRepository brukerRepository = mock(BrukerRepository.class);
        AktorClient aktorClient = mock(AktorClient.class);
        when(aktorClient.hentFnr(any())).thenReturn(Fnr.of("-1"));
        when(brukerRepository.retrievePersonidFromFnr(Fnr.of("-1"))).thenReturn(Optional.of(PersonId.of("0000")));
        this.oppfolgingStartetService = new OppfolgingStartetService(oppfolgingRepository, mock(OppfolgingRepositoryV2.class), mock(OpensearchIndexer.class), brukerRepository, aktorClient);
    }

    @Test
    void skal_sette_bruker_under_oppfølging_i_databasen() {
        final AktorId aktoerId = TestDataUtils.randomAktorId();

        final OppfolgingStartetDTO payload = new OppfolgingStartetDTO(aktoerId, ZonedDateTime.parse("2020-12-01T00:00:00+02:00"));

        oppfolgingStartetService.behandleKafkaMeldingLogikk(payload);

        final BrukerOppdatertInformasjon info = oppfolgingRepository.hentOppfolgingData(aktoerId).orElseThrow();
        assertThat(info.getOppfolging()).isTrue();
        assertThat(info.getNyForVeileder()).isFalse();
    }
}
