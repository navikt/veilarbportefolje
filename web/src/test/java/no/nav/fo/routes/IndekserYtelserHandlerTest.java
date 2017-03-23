package no.nav.fo.routes;

import no.nav.fo.consumer.IndekserYtelserHandler;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.service.SolrService;
import no.nav.melding.virksomhet.loependeytelser.v1.*;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyList;
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

    @BeforeClass
    public static void before() {
        System.setProperty("disable.metrics.report", "true");
    }

    @Before
    public void setup() {
        when(brukerRepository.retrievePersonidFromFnrs(anyCollection())).then((invocationOnMock -> {
            Collection<String> fnrs = (Collection<String>) invocationOnMock.getArguments()[0];
            Map<String, Optional<SolrInputDocument>> res = fnrs.stream()
                    .collect(Collectors.toMap(identity(), (fnr) -> {
                        if ("10108000398".equals(fnr)) {
                            return Optional.empty();
                        }
                        return Optional.of(new SolrInputDocument());
                    }));
            return res;
        }));

    }

    @Test
    public void filtrerBortBrukereSomIkkeErIDatabasen() throws Exception {
        LoependeYtelser ytelser = lagLoependeYtelser(asList(
                lagVedtak("10108000398"), // Skal bli filtrert bort
                lagVedtak("10108000399")
        ));

        handler.indekser(ytelser);

        verify(solr, times(1)).addDocuments(anyList());
        verify(solr).addDocuments(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    public void leggerTilManedFasetteringOmUtlopsdatoErInnenforSammeKalenderar() throws Exception {
        LoependeYtelser ytelser = lagLoependeYtelser(asList(
                lagVedtak("10108000397", new BigInteger("52"), BigInteger.TEN), // Utenfor kalenderåret
                lagVedtak("10108000399")
        ));

        handler.indekser(ytelser);

        verify(solr, times(1)).addDocuments(anyList());
        verify(solr).addDocuments(captor.capture());

        List<SolrInputDocument> solrDokumenter = captor.getValue();
        assertThat(solrDokumenter).hasSize(2);

        assertThat(solrDokumenter.get(0).keySet()).containsExactly("ytelse", "utlopsdato");
        assertThat(solrDokumenter.get(1).keySet()).containsExactly("ytelse", "utlopsdato", "utlopsdato_mnd_fasett");
    }

    @Test
    public void leggerTilAAPMAXTid() {
        LoependeYtelser ytelser = lagLoependeYtelser(asList(
                lagVedtak("10108000397", "AA", "AAP"),
                lagVedtak("10108000399", "AA", "AAP", new BigInteger("205"), BigInteger.TEN)
        ));

        handler.indekser(ytelser);

        verify(solr, times(1)).addDocuments(anyList());
        verify(solr).addDocuments(captor.capture());

        List<SolrInputDocument> solrDokumenter = captor.getValue();
        assertThat(solrDokumenter).hasSize(2);

        assertThat(solrDokumenter.get(0).keySet()).containsExactly("ytelse", "utlopsdato", "utlopsdato_mnd_fasett", "aap_maxtid", "aap_maxtid_fasett");
        assertThat(solrDokumenter.get(1).keySet()).containsExactly("ytelse", "utlopsdato", "utlopsdato_mnd_fasett", "aap_maxtid");
    }

    @Test
    public void leggerIkkeTilAAPMaxTilVedAAPUnntak() {
        LoependeVedtak vedtak = lagVedtak("10108000397", "AA", "AAP");
        vedtak.getAaptellere().setAntallDagerUnntak(BigInteger.ONE);
        LoependeYtelser ytelser = lagLoependeYtelser(asList(vedtak));

        handler.indekser(ytelser);

        verify(solr, times(1)).addDocuments(anyList());
        verify(solr).addDocuments(captor.capture());

        List<SolrInputDocument> solrDokumenter = captor.getValue();
        assertThat(solrDokumenter).hasSize(1);

        assertThat(solrDokumenter.get(0).keySet()).containsExactly("ytelse", "utlopsdato", "utlopsdato_mnd_fasett");
    }

    private LoependeYtelser lagLoependeYtelser(List<LoependeVedtak> vedtak) {
        LoependeYtelser ytelser = new LoependeYtelser();

        ytelser.getLoependeVedtakListe().addAll(vedtak);

        return ytelser;
    }

    private LoependeVedtak lagVedtak(String fnr) {
        return lagVedtak(fnr, "DAGP", "DAGO", BigInteger.ONE, BigInteger.ONE);
    }

    private LoependeVedtak lagVedtak(String fnr, BigInteger uker, BigInteger dager) {
        return lagVedtak(fnr, "DAGP", "DAGO", uker, dager);
    }

    private LoependeVedtak lagVedtak(String fnr, String sakstype, String rettighetstype) {
        return lagVedtak(fnr, sakstype, rettighetstype, BigInteger.ONE, BigInteger.ONE);
    }

    private LoependeVedtak lagVedtak(String fnr, String sakstype, String rettighetstype, BigInteger uker, BigInteger dager) {
        LoependeVedtak vedtak = new LoependeVedtak();

        Dagpengetellere dagpengetellere = new Dagpengetellere();
        dagpengetellere.setAntallUkerIgjen(uker);
        dagpengetellere.setAntallDagerIgjen(dager);


        vedtak.setPersonident(fnr);
        vedtak.setSakstypeKode(sakstype);
        vedtak.setRettighetstypeKode(rettighetstype);
        vedtak.setDagpengetellere(dagpengetellere);

        if (!"DAGP".equals(sakstype)) {
            AAPtellere aaptellere = new AAPtellere();
            aaptellere.setAntallUkerIgjen(uker);
            aaptellere.setAntallDagerIgjen(dager);
            vedtak.setAaptellere(aaptellere);

            try {
                Periode periode = new Periode();
                LocalDate now = LocalDate.now().plusDays(10);
                GregorianCalendar gcal = GregorianCalendar.from(now.atStartOfDay(ZoneId.systemDefault()));
                XMLGregorianCalendar xcal = null;
                xcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);

                periode.setTom(xcal);
                vedtak.setVedtaksperiode(periode);
            } catch (DatatypeConfigurationException e) {
                throw new RuntimeException(e);
            }
        }

        return vedtak;
    }
}