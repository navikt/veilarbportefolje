package no.nav.pto.veilarbportefolje.arenapakafka;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.aktiviteter.*;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.GruppeAktivitetRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.GruppeAktivitetRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV2;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV3;
import no.nav.pto.veilarbportefolje.arenapakafka.arenaDTO.TiltakInnhold;
import no.nav.pto.veilarbportefolje.database.BrukerDataRepository;
import no.nav.pto.veilarbportefolje.database.BrukerDataService;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;

public class TiltakPostgresTest {
    private JdbcTemplate db;
    private TiltakRepositoryV3 tiltakRepositoryV3;
    private AktivitetStatusRepositoryV2 aktivitetStatusRepositoryV2;
    private BrukerDataService brukerDataService;

    private final AktorId aktorId = AktorId.of("123");
    private final Fnr fnr = Fnr.of("12345678912");
    private final PersonId personId = PersonId.of("123");

    @Before
    public void setup() {
        db = SingletonPostgresContainer.init().createJdbcTemplate();
        aktivitetStatusRepositoryV2 = new AktivitetStatusRepositoryV2(db);
        tiltakRepositoryV3 = new TiltakRepositoryV3(db, aktivitetStatusRepositoryV2);
        GruppeAktivitetRepositoryV2 gruppeAktivitetRepositoryV2 = mock(GruppeAktivitetRepositoryV2.class);
        AktiviteterRepositoryV2 aktiviteterRepositoryV2 = mock(AktiviteterRepositoryV2.class);
        Mockito.when(gruppeAktivitetRepositoryV2.hentAktiveAktivteter(any())).thenReturn(new ArrayList<>());
        Mockito.when(aktiviteterRepositoryV2.getAvtalteAktiviteterForAktoerid(any())).thenReturn(new AktoerAktiviteter("1").setAktiviteter(new ArrayList<>()));
        brukerDataService = new BrukerDataService(mock(AktivitetDAO.class), mock(TiltakRepositoryV2.class), tiltakRepositoryV3, mock(GruppeAktivitetRepository.class), gruppeAktivitetRepositoryV2, mock(BrukerDataRepository.class), aktiviteterRepositoryV2, aktivitetStatusRepositoryV2);
    }

    @Test
    public void skal_lagre_tiltak_i_kodeverk_og_på_bruker() {
        String tiltaksType = "T123";
        String tiltaksNavn = "test";
        TiltakInnhold innhold = new TiltakInnhold()
                .setFnr(fnr.get())
                .setPersonId(personId.toInteger())
                .setHendelseId(1)
                .setTiltaksnavn(tiltaksNavn)
                .setTiltakstype(tiltaksType)
                .setDeltakerStatus("GJENN")
                .setEndretDato(new ArenaDato("2021-01-01"))
                .setAktivitetperiodeTil(new ArenaDato("1990-01-01"))
                .setAktivitetid("TA-123456789");
        tiltakRepositoryV3.upsert(innhold, aktorId);

        tiltakRepositoryV3.utledOgLagreTiltakInformasjon(aktorId);
        brukerDataService.oppdaterAktivitetBrukerDataPostgres(aktorId);

        List<String> tiltak = tiltakRepositoryV3.hentBrukertiltak(aktorId);
        Optional<AktivitetStatus> aktivitetStatus = aktivitetStatusRepositoryV2.hentAktivitetTypeStatus(aktorId.get(), AktivitetTyper.tiltak.name());
        Optional<Timestamp> utloptAktivitet = aktivitetStatusRepositoryV2.hentAktivitetStatusUtlopt(aktorId.get());

        Optional<String> kodeVerkNavn = tiltakRepositoryV3.hentVerdiITiltakskodeVerk(tiltaksType);

        assertThat(tiltak.size()).isEqualTo(1);
        assertThat(tiltak.get(0)).isEqualTo(tiltaksType);

        assertThat(aktivitetStatus.isPresent()).isTrue();
        assertThat(aktivitetStatus.get().isAktiv()).isTrue();
        assertThat(aktivitetStatus.get().getNesteUtlop()).isNull();

        assertThat(kodeVerkNavn.isPresent()).isTrue();
        assertThat(kodeVerkNavn.get()).isEqualTo(tiltaksNavn);

        assertThat(utloptAktivitet.isPresent()).isTrue();
        assertThat(utloptAktivitet.get().toLocalDateTime().getYear()).isEqualTo(1990);
    }

    @Test
    public void skal_lage_slette_tiltak_på_bruker_men_ikke_kodeverk() {
        String tiltaksType = "T123";
        String tiltaksNavn = "test";
        String id = "TA-123456789";
        TiltakInnhold innhold = new TiltakInnhold()
                .setFnr(fnr.get())
                .setPersonId(personId.toInteger())
                .setHendelseId(1)
                .setTiltaksnavn(tiltaksNavn)
                .setTiltakstype(tiltaksType)
                .setDeltakerStatus("GJENN")
                .setEndretDato(new ArenaDato("2021-01-01"))
                .setAktivitetid(id);
        tiltakRepositoryV3.upsert(innhold, aktorId);
        tiltakRepositoryV3.delete(id);

        tiltakRepositoryV3.utledOgLagreTiltakInformasjon(aktorId);
        brukerDataService.oppdaterAktivitetBrukerDataPostgres(aktorId);

        List<String> tiltak = tiltakRepositoryV3.hentBrukertiltak(aktorId);
        Optional<AktivitetStatus> aktivitetStatus = aktivitetStatusRepositoryV2.hentAktivitetTypeStatus(aktorId.get(), AktivitetTyper.tiltak.name());
        Optional<Timestamp> utloptAktivitet = aktivitetStatusRepositoryV2.hentAktivitetStatusUtlopt(aktorId.get());

        Optional<String> kodeVerkNavn = tiltakRepositoryV3.hentVerdiITiltakskodeVerk(tiltaksType);

        assertThat(tiltak.size()).isEqualTo(0);
        assertThat(kodeVerkNavn.isPresent()).isTrue();
        assertThat(kodeVerkNavn.get()).isEqualTo(tiltaksNavn);

        assertThat(aktivitetStatus.isPresent()).isTrue();
        assertThat(aktivitetStatus.get().isAktiv()).isFalse();
        assertThat(utloptAktivitet.isPresent()).isFalse();
    }

    //TODO: test på enhet
}
