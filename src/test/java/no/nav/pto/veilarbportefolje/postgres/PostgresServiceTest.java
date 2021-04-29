package no.nav.pto.veilarbportefolje.postgres;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.dialog.DialogRepositoryV2;
import no.nav.pto.veilarbportefolje.dialog.Dialogdata;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerKafkaDTO;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolginsbrukerRepositoryV2;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import no.nav.pto.veilarbportefolje.util.VedtakstottePilotRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.List;

import static no.nav.pto.veilarbportefolje.domene.Brukerstatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PostgresServiceTest {
    private PostgresService postgresService;
    private VeilarbVeilederClient veilarbVeilederClient;
    private DialogRepositoryV2 dialogRepositoryV2;
    private OppfolgingRepositoryV2 oppfolgingRepositoryV2;
    private OppfolginsbrukerRepositoryV2 oppfolginsbrukerRepositoryV2;

    private final String enhetId = "1234";

    @Before
    public void setup() {
        JdbcTemplate db = SingletonPostgresContainer.init().createJdbcTemplate();
        dialogRepositoryV2 = new DialogRepositoryV2(db);
        oppfolgingRepositoryV2 = new OppfolgingRepositoryV2(db);
        oppfolginsbrukerRepositoryV2 = new OppfolginsbrukerRepositoryV2(db);

        VedtakstottePilotRequest vedtakstottePilotRequest =  mock(VedtakstottePilotRequest.class);
        veilarbVeilederClient = mock(VeilarbVeilederClient.class);

        postgresService = new PostgresService(vedtakstottePilotRequest, db, veilarbVeilederClient);
    }

    @Test
    public void sok_resulterer_i_ingen_brukere(){
        when(veilarbVeilederClient.hentVeilederePaaEnhet(any())).thenReturn(List.of("Z12345","Z12346"));

        Filtervalg filtervalg = new Filtervalg().setFerdigfilterListe(List.of(UFORDELTE_BRUKERE));
        postgresService.hentBrukere("1234",null, null,null, filtervalg, 0, 10);

    }

    @Test
    public void sok_pa_dialog(){
        AktorId aktorId = AktorId.of("123456789");
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now());
        oppfolginsbrukerRepositoryV2.LeggTilEllerEndreOppfolgingsbruker(new OppfolgingsbrukerKafkaDTO().setAktoerid(aktorId.get()).setNav_kontor(enhetId).setEndret_dato(ZonedDateTime.now()));
        ZonedDateTime venter_tidspunkt = ZonedDateTime.now();
        dialogRepositoryV2.oppdaterDialogInfoForBruker(
                new Dialogdata()
                        .setAktorId(aktorId.get())
                        .setSisteEndring(ZonedDateTime.now())
                        .setTidspunktEldsteVentende(venter_tidspunkt));

        when(veilarbVeilederClient.hentVeilederePaaEnhet(any())).thenReturn(List.of("Z12345","Z12346"));
        Filtervalg filtervalg = new Filtervalg().setFerdigfilterListe(List.of(VENTER_PA_SVAR_FRA_BRUKER));

        BrukereMedAntall brukereMedAntall = postgresService.hentBrukere(enhetId, null, null, null, filtervalg, 0, 10);
        assertThat(brukereMedAntall.getAntall()).isEqualTo(1);
        assertThat(brukereMedAntall.getBrukere().get(0).getVenterPaSvarFraBruker()).isEqualTo(venter_tidspunkt.toLocalDateTime());
    }
}
