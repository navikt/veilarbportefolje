package no.nav.fo.routes;

import no.nav.fo.database.BrukerRepository;
import no.nav.fo.service.SolrService;
import no.nav.melding.virksomhet.loependeytelser.v1.Dagpengetellere;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeVedtak;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class IndekserYtelserHandlerTest {

    @Mock
    SolrService solr;

    @Mock
    BrukerRepository brukerRepository;

    @InjectMocks
    IndekserYtelserHandler handler;

    @Captor
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

    @Before
    public void setup() {
        when(brukerRepository.retrievePersonidFromFnr(anyString()))
                .then(invocationOnMock -> {
                    String fnr = (String) invocationOnMock.getArguments()[0];

                    if ("10108000398".equals(fnr)) {
                        return Optional.empty();
                    }

                    return Optional.of(fnr).map(BigDecimal::new);
                });
    }

    @Test
    public void name() throws Exception {
        LoependeYtelser ytelser = lagLoependeYtelser();
        handler.indekser(ytelser);

        verify(solr, times(1)).addDocuments(anyList());
        verify(solr).addDocuments(captor.capture());

        assertThat(captor.getValue()).hasSize(1);
    }

    private LoependeYtelser lagLoependeYtelser() {
        LoependeYtelser ytelser = new LoependeYtelser();

        ytelser.getLoependeVedtakListe()
                .addAll(Arrays.asList(
                        lagVedtak("10108000398"),
                        lagVedtak("10108000399")
                ));

        return ytelser;
    }

    private LoependeVedtak lagVedtak(String fnr) {
        LoependeVedtak vedtak = new LoependeVedtak();
        Dagpengetellere dagpengetellere = new Dagpengetellere();
        dagpengetellere.setAntallUkerIgjen(BigInteger.ONE);
        dagpengetellere.setAntallDagerIgjen(BigInteger.ONE);

        vedtak.setPersonident(fnr);
        vedtak.setSakstypeKode("DAGP");
        vedtak.setRettighetstypeKode("DAGO");
        vedtak.setDagpengetellere(dagpengetellere);

        return vedtak;
    }
}