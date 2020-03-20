package no.nav.pto.veilarbportefolje.registrering;

import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.registrering.domene.Besvarelse;
import no.nav.pto.veilarbportefolje.registrering.domene.BrukerRegistreringWrapper;
import no.nav.pto.veilarbportefolje.registrering.domene.DinSituasjonSvar;
import no.nav.pto.veilarbportefolje.registrering.domene.OrdinaerBrukerRegistrering;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;

import static no.nav.pto.veilarbportefolje.config.LocalJndiContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PopulerDataFraRegistreringTest {
    private JdbcTemplate jdbcTemplate = new JdbcTemplate(setupInMemoryDatabase());
    private RegistreringRepository registreringRepository = new RegistreringRepository(jdbcTemplate);
    private RegistreringService registreringService = new RegistreringService(registreringRepository);
    private BrukerRepository brukerRepository = mock(BrukerRepository.class);
    private VeilarbregistreringClient veilarbregistreringClient;
    private PopulerDataFraRegistrering populerDataFraRegistrering;



    @Before
    public void setUp() {
        System.setProperty("VEILARBREGISTRERING_URL", "thisUrlMustBeSetAtLeastToADummyValue");
        this.veilarbregistreringClient = mock(VeilarbregistreringClient.class);

        when(brukerRepository.hentAlleBrukereUnderOppfolgingRegistrering()).thenReturn(Collections.singletonList(
                new OppfolgingsBruker()
                        .setFnr("12346789101")
                        .setAktoer_id("123456789")
                        .setOppfolging_startdato("2020-01-03T15:54:02.658Z")
        ));

        this.populerDataFraRegistrering = new PopulerDataFraRegistrering(registreringService, brukerRepository, veilarbregistreringClient);
        jdbcTemplate.execute("truncate table BRUKER_REGISTRERING");

    }


    @Test
    public void skallHanteraAttOppfolgingsBrukerenIkkeHarRegistrertSig() {
        when(veilarbregistreringClient.hentRegistrering(any(Fnr.class))).thenReturn(Try.success(null));
        AktoerId aktoerId = AktoerId.of("123456789");
        populerDataFraRegistrering.populerMedBrukerRegistrering(0,1);

        assertThat(registreringRepository.hentBrukerRegistrering(aktoerId)).isEqualTo(null);

    }

    @Test
    public void skallHanteraAttOppfolgingsBrukerenIkkeHarSituasjon() {
        OrdinaerBrukerRegistrering ordinaerBrukerRegistrering = new OrdinaerBrukerRegistrering();
        ordinaerBrukerRegistrering.setBesvarelse(null);
        BrukerRegistreringWrapper brukerRegistreringWrapper = new BrukerRegistreringWrapper().setRegistrering(ordinaerBrukerRegistrering);

        when(veilarbregistreringClient.hentRegistrering(any(Fnr.class))).thenReturn(Try.success(brukerRegistreringWrapper));
        AktoerId aktoerId = AktoerId.of("123456789");
        populerDataFraRegistrering.populerMedBrukerRegistrering(0,1);

        assertThat(registreringRepository.hentBrukerRegistrering(aktoerId)).isEqualTo(null);
    }

    @Test
    public void skallSetteInBrukereSomHarSituasjon() {
        OrdinaerBrukerRegistrering ordinaerBrukerRegistrering = new OrdinaerBrukerRegistrering();
        Besvarelse besvarelse = new Besvarelse();
        besvarelse.setDinSituasjon(DinSituasjonSvar.ER_PERMITTERT);

        ordinaerBrukerRegistrering.setBesvarelse(besvarelse);
        BrukerRegistreringWrapper brukerRegistreringWrapper = new BrukerRegistreringWrapper().setRegistrering(ordinaerBrukerRegistrering);

        when(veilarbregistreringClient.hentRegistrering(any(Fnr.class))).thenReturn(Try.success(brukerRegistreringWrapper));
        AktoerId aktoerId = AktoerId.of("123456789");
        populerDataFraRegistrering.populerMedBrukerRegistrering(0,1);

        assertThat(registreringRepository.hentBrukerRegistrering(aktoerId).getBrukersSituasjon()).isEqualTo(DinSituasjonSvar.ER_PERMITTERT.name());
    }
}
