package no.nav.fo.consumer;

import io.vavr.control.Try;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.*;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import no.nav.fo.service.SolrService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.Mock;


import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TilordningFeedHandlerTest {

    @Mock
    private OppdaterBrukerdataFletter oppdaterBrukerdataFletter;

    @Mock
    private ArbeidslisteService arbeidslisteService;

    @Mock
    private BrukerRepository brukerRepository;

    @Mock
    private AktoerService aktoerService;

    @Mock
    private SolrService solrService;

    @InjectMocks
    private TilordningFeedHandler tilordningFeedHandler;

    @Before
    public void resetMocks() {
        reset(oppdaterBrukerdataFletter,arbeidslisteService,brukerRepository,aktoerService,solrService);
    }

    @Test
    public void skalSletteBrukerOgArbeidslisteNaarDenIkkeErUnderOppfolging() {
        Fnr fnr = new Fnr("00000000000");
        AktoerId aktoerId = new AktoerId("DUMMY");
        PersonId personId = new PersonId("1111111");


        Try<Oppfolgingstatus> arenaStatus = Try.success(new Oppfolgingstatus()
                .setFormidlingsgruppekode("DUMMY")
                .setServicegruppekode("DUMMY"));

        BrukerOppdatertInformasjon bruker = new BrukerOppdatertInformasjon()
                .setOppfolging(false)
                .setAktoerid(aktoerId.toString());

        when(brukerRepository.retrieveOppfolgingstatus(any())).thenReturn(arenaStatus);
        when(aktoerService.hentFnrFraAktoerid(any())).thenReturn(Try.success(fnr));
        when(aktoerService.hentPersonidFraAktoerid(any())).thenReturn(Try.success(personId));

        tilordningFeedHandler.call("1970-01-01T00:00:00Z", Collections.singletonList(bruker));

        verify(arbeidslisteService, times(1)).deleteArbeidsliste(aktoerId);
        verify(solrService, times(1)).slettBruker(personId);
        verify(oppdaterBrukerdataFletter, never()).tilordneVeilederTilPersonId(any());
    }


}