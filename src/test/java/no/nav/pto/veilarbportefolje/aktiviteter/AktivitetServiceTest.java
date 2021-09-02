package no.nav.pto.veilarbportefolje.aktiviteter;

import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.util.AktivitetData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AktivitetServiceTest {

    @Mock
    private BrukerService brukerService;

    @Mock
    private AktivitetDAO aktivitetDAO;

    @Mock
    private PersistentOppdatering persistentOppdatering;

    @Mock
    private UnleashService unleashService;

    @InjectMocks
    private AktivitetService aktivitetService;

    @Before
    public void resetMock() {
        reset(brukerService, persistentOppdatering, aktivitetDAO, unleashService);
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

        ArgumentCaptor<List<AktivitetBrukerOppdatering>> captor = ArgumentCaptor.forClass((Class) List.class);

        String aktivitettype = AktivitetData.aktivitetTyperList.get(0).toString();

        String ikkeFullfortStatus = "ikkeFullfortStatus";

        when(aktivitetDAO.getDistinctAktorIdsFromAktivitet()).thenReturn(aktoerids);
        when(unleashService.isEnabled(any())).thenReturn(true);

        when(aktivitetDAO.getAvtalteAktiviteterForAktoerid(any(AktorId.class))).thenAnswer(invocationOnMock -> {
                    AktorId aktoer = (AktorId) invocationOnMock.getArguments()[0];
                    return new AktoerAktiviteter(aktoer.toString()).setAktiviteter(singletonList(new AktivitetDTO()
                            .setTilDato(Timestamp.from(Instant.now()))
                            .setFraDato(Timestamp.from(Instant.now()))
                            .setAktivitetType(aktivitettype)
                            .setStatus(ikkeFullfortStatus)));
                });

        when(brukerService
                .hentPersonidFraAktoerid(any(AktorId.class)))
                .thenAnswer(args -> Try.success(PersonId.of(args.getArgument(0).toString())));

        aktivitetService.utledOgLagreAlleAktivitetstatuser();

        verify(persistentOppdatering, times(antallPersoner)).lagreBrukeroppdateringerIDB(captor.capture());

        List<String> capturedAktoerids = captor.getAllValues()
                .stream()
                .flatMap(Collection::stream)
                .map(AktivitetBrukerOppdatering::getAktoerid)
                .collect(Collectors.toList());

        assertThat(capturedAktoerids).containsAll(aktoerids);
    }


    @Test
    public void brukerMedEnAvtaltAktivAktivitetHovedindeksering() {
        AktoerAktiviteter aktiviteter = new AktoerAktiviteter(AKTOERID_TEST);
        Timestamp tilDato = Timestamp.valueOf(LocalDate.now().plusDays(10).atStartOfDay());
        String aktivitetType = AktivitetTyper.mote.toString();

        aktiviteter.setAktiviteter(singletonList(new AktivitetDTO()
                .setTilDato(tilDato)
                .setAktivitetType(aktivitetType)));

        ArgumentCaptor<List<AktivitetBrukerOppdatering>> captor = ArgumentCaptor.forClass((Class) List.class);

        when(aktivitetDAO.getDistinctAktorIdsFromAktivitet()).thenReturn(singletonList(AKTOERID_TEST));
        when(aktivitetDAO.getAvtalteAktiviteterForAktoerid(any())).thenReturn(aktiviteter);
        when(brukerService.hentPersonidFraAktoerid(any())).thenReturn(Try.success(PersonId.of(PERSONID_TEST)));
        when(unleashService.isEnabled(any())).thenReturn(true);

        aktivitetService.utledOgLagreAlleAktivitetstatuser();

        verify(persistentOppdatering, times(1)).lagreBrukeroppdateringerIDB(captor.capture());
        Timestamp capturedDate = captor.getValue().get(0).getAktiviteter().stream()
                .filter(a -> a.getAktivitetType().equals(aktivitetType))
                .findFirst().get().getNesteUtlop();

        assertThat(capturedDate).isEqualTo(tilDato);
    }
}
