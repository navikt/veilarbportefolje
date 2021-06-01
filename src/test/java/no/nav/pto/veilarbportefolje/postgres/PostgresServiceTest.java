package no.nav.pto.veilarbportefolje.postgres;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteDTO;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteRepositoryV2;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.dialog.DialogRepositoryV2;
import no.nav.pto.veilarbportefolje.dialog.Dialogdata;
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.Kjonn;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerKafkaDTO;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolginsbrukerRepositoryV2;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import no.nav.pto.veilarbportefolje.util.VedtakstottePilotRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
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
    private ArbeidslisteRepositoryV2 arbeidslisteRepositoryV2;

    private final String enhetId = "1234";

    @Before
    public void setup() {
        JdbcTemplate db = SingletonPostgresContainer.init().createJdbcTemplate();
        dialogRepositoryV2 = new DialogRepositoryV2(db);
        oppfolgingRepositoryV2 = new OppfolgingRepositoryV2(db);
        oppfolginsbrukerRepositoryV2 = new OppfolginsbrukerRepositoryV2(db);
        arbeidslisteRepositoryV2 = new ArbeidslisteRepositoryV2(db);

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
    public void sok_pa_tekst(){
        when(veilarbVeilederClient.hentVeilederePaaEnhet(any())).thenReturn(List.of("Z12345","Z12346"));

        Filtervalg teskt = new Filtervalg().setNavnEllerFnrQuery("test");
        Filtervalg fnr = new Filtervalg().setNavnEllerFnrQuery("123");
        postgresService.hentBrukere("1234",null, null,null, teskt, 0, 10);
        postgresService.hentBrukere("1234",null, null,null, fnr, 0, 10);
    }


    @Test
    public void sok_pa_arbeidslista(){
        when(veilarbVeilederClient.hentVeilederePaaEnhet(any())).thenReturn(List.of("Z12345","Z12346"));
        AktorId aktorId = AktorId.of("123456789");
        Fnr fnr = Fnr.ofValidFnr("01010101010");
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now());
        oppfolginsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(new OppfolgingsbrukerKafkaDTO().setAktoerid(aktorId.get()).setNav_kontor(enhetId).setEndret_dato(ZonedDateTime.now()).setSperret_ansatt(true));

        Filtervalg filtervalg = new Filtervalg().setFerdigfilterListe(List.of(MIN_ARBEIDSLISTE));
        BrukereMedAntall brukereMedAntall_pre = postgresService.hentBrukere(enhetId, null, null, null, filtervalg, 0, 10);
        assertThat(brukereMedAntall_pre.getAntall()).isEqualTo(0);

        arbeidslisteRepositoryV2.insertArbeidsliste(new ArbeidslisteDTO(fnr)
                .setAktorId(aktorId)
                .setVeilederId(VeilederId.of("X11111"))
                .setFrist(Timestamp.from(Instant.parse("2017-10-11T00:00:00Z")))
                .setKommentar("Dette er en kommentar")
                .setOverskrift("Dette er en overskrift")
                .setKategori(Arbeidsliste.Kategori.BLA));

        BrukereMedAntall brukereMedAntall_post = postgresService.hentBrukere(enhetId, null, null, null, filtervalg, 0, 10);
        assertThat(brukereMedAntall_post.getAntall()).isEqualTo(1);
        assertThat(brukereMedAntall_post.getBrukere().get(0).getArbeidsliste().getOverskrift()).isEqualTo("Dette er en overskrift");
    }

    @Test
    public void skal_filtrere_pa_kjonn(){
        when(veilarbVeilederClient.hentVeilederePaaEnhet(any())).thenReturn(List.of("Z12345","Z12346"));
        lastOppBruker(Fnr.of("12031240241"), AktorId.of("123")); // Kvinne
        lastOppBruker(Fnr.of("12031240141"), AktorId.of("321")); // Mann

        Filtervalg filtervalg_kvinne = new Filtervalg().setFerdigfilterListe(List.of()).setKjonn(Kjonn.K);
        Filtervalg filtervalg_mann = new Filtervalg().setFerdigfilterListe(List.of()).setKjonn(Kjonn.M);

        BrukereMedAntall kvinne_respons = postgresService.hentBrukere(enhetId, null, null, null, filtervalg_kvinne, 0, 10);
        BrukereMedAntall mann_respons = postgresService.hentBrukere(enhetId, null, null, null, filtervalg_mann, 0, 10);

        assertThat(kvinne_respons.getAntall()).isEqualTo(1);
        assertThat(kvinne_respons.getBrukere().get(0).getKjonn()).isEqualTo("K");

        assertThat(mann_respons.getAntall()).isEqualTo(1);
        assertThat(mann_respons.getBrukere().get(0).getKjonn()).isEqualTo("M");
    }

    @Test
    public void skal_filtrere_pa_alder(){
        when(veilarbVeilederClient.hentVeilederePaaEnhet(any())).thenReturn(List.of("Z12345","Z12346"));
        lastOppBruker(Fnr.of("01091964488"), AktorId.of("123")); // under_21
        lastOppBruker(Fnr.of("09118714501"), AktorId.of("321")); // Mann: 33

        Filtervalg alder_type_1 = new Filtervalg().setFerdigfilterListe(List.of()).setAlder(List.of("19-og-under"));
        Filtervalg alder_type_2 = new Filtervalg().setFerdigfilterListe(List.of()).setAlder(List.of("20-24", "30-39"));

        BrukereMedAntall alder_respons_type_1 = postgresService.hentBrukere(enhetId, null, null, null, alder_type_1, 0, 10);
        BrukereMedAntall alder_respons_type_2 = postgresService.hentBrukere(enhetId, null, null, null, alder_type_2, 0, 10);

        assertThat(alder_respons_type_1.getAntall()).isEqualTo(1);
        assertThat(alder_respons_type_2.getAntall()).isEqualTo(1);
        assertThat(alder_respons_type_2.getBrukere().get(0).getFnr()).isEqualTo("09118714501");
    }

    @Test
    public void skal_filtrere_pa_fodselsdag(){
        when(veilarbVeilederClient.hentVeilederePaaEnhet(any())).thenReturn(List.of("Z12345","Z12346"));
        lastOppBruker(Fnr.of("01091964488"), AktorId.of("123")); // 1 i maneden
        lastOppBruker(Fnr.of("07091964488"), AktorId.of("321")); // 7 i maneden

        Filtervalg alder_type_1 = new Filtervalg().setFerdigfilterListe(List.of()).setFodselsdagIMnd(List.of("1"));
        Filtervalg alder_type_2 = new Filtervalg().setFerdigfilterListe(List.of()).setFodselsdagIMnd(List.of("1","7"));

        BrukereMedAntall alder_respons_type_1 = postgresService.hentBrukere(enhetId, null, null, null, alder_type_1, 0, 10);
        BrukereMedAntall alder_respons_type_2 = postgresService.hentBrukere(enhetId, null, null, null, alder_type_2, 0, 10);

        assertThat(alder_respons_type_1.getAntall()).isEqualTo(1);
        assertThat(alder_respons_type_2.getAntall()).isEqualTo(2);
        assertThat(alder_respons_type_1.getBrukere().get(0).getFnr()).isEqualTo("01091964488");
    }

    @Test
    public void sok_pa_dialog(){
        AktorId aktorId = AktorId.of("123456789");
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now());
        oppfolginsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(new OppfolgingsbrukerKafkaDTO().setAktoerid(aktorId.get()).setNav_kontor(enhetId).setEndret_dato(ZonedDateTime.now()).setSperret_ansatt(true));
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

    private void lastOppBruker(Fnr fnr, AktorId aktorId){
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now());
        oppfolginsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(new OppfolgingsbrukerKafkaDTO().setAktoerid(aktorId.get()).setNav_kontor(enhetId).setEndret_dato(ZonedDateTime.now()).setSperret_ansatt(true).setFodselsnr(fnr.get()));
        ZonedDateTime venter_tidspunkt = ZonedDateTime.now();
    }
}
