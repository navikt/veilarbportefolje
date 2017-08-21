package no.nav.fo.service;

import io.vavr.control.Try;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.BrukerOppdatering;
import no.nav.fo.domene.PersonId;
import no.nav.fo.domene.aktivitet.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AktivitetServiceTest {

    @Mock
    private AktoerService aktoerService;

    @Mock
    private BrukerRepository brukerRepository;

    @Mock
    private PersistentOppdatering persistentOppdatering;

    @InjectMocks
    private AktivitetService aktivitetService;

    @Before
    public void resetMock() {
        reset(aktoerService, brukerRepository, persistentOppdatering);
    }

    private static final String AKTOERID_TEST = "AKTOERID_TEST";
    private static final String PERSONID_TEST = "PERSONID_TEST";

    @Test
    public void skalKalleLagreForAlleAktoerider() {
        int antallPersoner = 16259;

        List<String> aktoerids = new ArrayList<>(antallPersoner);
        for (int i = 0; i < antallPersoner; i++) {
            aktoerids.add("aktoerid" + Integer.toString(i));
        }

        ArgumentCaptor<AktivitetBrukerOppdatering> captor = ArgumentCaptor.forClass(AktivitetBrukerOppdatering.class);

        String aktivitettype = AktivitetData.aktivitetTyperList.get(0).toString();

        String ikkeFullfortStatus = "ikkeFullfortStatus";

        when(brukerRepository.getDistinctAktoerIdsFromAktivitet()).thenReturn(aktoerids);

        when(brukerRepository.getAktiviteterForListOfAktoerid(anyList())).thenAnswer(invocationOnMock -> {
            List<String> aktorer = (ArrayList<String>) invocationOnMock.getArguments()[0];
            return aktorer
                    .stream()
                    .map(aktoer -> {
                        AktivitetDTO aktivitet = new AktivitetDTO()
                                .setTilDato(Timestamp.from(Instant.now()))
                                .setFraDato(Timestamp.from(Instant.now()))
                                .setAktivitetType(aktivitettype)
                                .setStatus(ikkeFullfortStatus);
                        return new AktoerAktiviteter(aktoer).setAktiviteter(singletonList(aktivitet));
                    })
                    .collect(Collectors.toList());
        });

        when(aktoerService
                .hentPersonidFraAktoerid(any(AktoerId.class)))
                .thenAnswer(args -> Try.success(new PersonId(args.getArgumentAt(0, AktoerId.class).toString())));

        aktivitetService.utledOgLagreAlleAktivitetstatuser();

        verify(persistentOppdatering, times(antallPersoner)).hentDataOgLagre(captor.capture());

        List<String> capturedAktoerids = captor.getAllValues()
                .stream()
                .map(AktivitetBrukerOppdatering::getAktoerid)
                .collect(Collectors.toList());

        assertThat(capturedAktoerids).containsAll(aktoerids);
    }

    @Test
    public void brukerMedEnAvtaltAktivAktivitet() {
        AktoerAktiviteter aktiviteter = new AktoerAktiviteter(AKTOERID_TEST);
        aktiviteter.setAktiviteter(singletonList(new AktivitetDTO()
                .setTilDato(Timestamp.valueOf(LocalDate.now().plusDays(10).atStartOfDay()))
                .setAktivitetType(AktivitetTyper.mote.toString())));

        ArgumentCaptor<BrukerOppdatering> captor = ArgumentCaptor.forClass(BrukerOppdatering.class);

        when(brukerRepository.getAktiviteterForListOfAktoerid(any())).thenReturn(singletonList(aktiviteter));
        when(aktoerService.hentPersonidFraAktoerid(any())).thenReturn(Try.success(new PersonId(PERSONID_TEST)));
        aktivitetService.utledOgLagreAktivitetstatuser(singletonList(AKTOERID_TEST));

        verify(persistentOppdatering, times(1)).hentDataOgLagre(captor.capture());
        AktivitetBrukerOppdatering oppdatering = (AktivitetBrukerOppdatering) captor.getValue();
        assertThat(oppdatering.getIAvtaltAktivitet()).isEqualTo(true);
    }

    @Test
    public void brukerMedEnAvtaltAktivAktivitetHovedindeksering() {
        AktoerAktiviteter aktiviteter = new AktoerAktiviteter(AKTOERID_TEST);
        aktiviteter.setAktiviteter(singletonList(new AktivitetDTO()
                .setTilDato(Timestamp.valueOf(LocalDate.now().plusDays(10).atStartOfDay()))
                .setAktivitetType(AktivitetTyper.mote.toString())));

        ArgumentCaptor<BrukerOppdatering> captor = ArgumentCaptor.forClass(BrukerOppdatering.class);

        when(brukerRepository.getDistinctAktoerIdsFromAktivitet()).thenReturn(singletonList(AKTOERID_TEST));
        when(brukerRepository.getAktiviteterForListOfAktoerid(any())).thenReturn(singletonList(aktiviteter));
        when(aktoerService.hentPersonidFraAktoerid(any())).thenReturn(Try.success(new PersonId(PERSONID_TEST)));

        aktivitetService.utledOgLagreAlleAktivitetstatuser();

        verify(persistentOppdatering, times(1)).hentDataOgLagre(captor.capture());
        AktivitetBrukerOppdatering oppdatering = (AktivitetBrukerOppdatering) captor.getValue();
        assertThat(oppdatering.getIAvtaltAktivitet()).isEqualTo(true);
    }
}