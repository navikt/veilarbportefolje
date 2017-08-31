package no.nav.fo.routes;

import no.nav.fo.filmottak.ytelser.IndekserYtelserHandler;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.BrukerinformasjonFraFil;
import no.nav.fo.domene.YtelseMapping;
import no.nav.melding.virksomhet.loependeytelser.v1.*;
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
    PersistentOppdatering persistentOppdatering;

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
            Map<String, Optional<String>> res = fnrs.stream()
                    .collect(Collectors.toMap(identity(), (fnr) -> {
                        if ("10108000398".equals(fnr)) { //TESTFAMILIE
                            return Optional.empty();
                        }
                        return Optional.of("anotherPersonId");
                    }));
            return res;
        }));

    }

    @Test
    public void filtrerBortBrukereSomIkkeErIDatabasen() throws Exception {
        LoependeYtelser ytelser = lagLoependeYtelser(asList(
                lagVedtak("10108000398"), //TESTFAMILIE // Skal bli filtrert bort
                lagVedtak("10108000399") //TESTFAMILIE
        ));

        handler.indekser(ytelser);

        verify(persistentOppdatering, times(1)).lagreBrukeroppdateringerIDB(anyList());
        verify(persistentOppdatering).lagreBrukeroppdateringerIDB(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    public void leggerTilManedFasetteringOmUtlopsdatoErInnenforSammeKalenderar() throws Exception {
        LoependeYtelser ytelser = lagLoependeYtelser(asList(
                lagVedtak("10108000397", new BigInteger("53"), BigInteger.TEN), //TESTFAMILIE // Utenfor kalenderåret
                lagVedtak("10108000399", "AA", "AAP") //TESTFAMILIE
        ));

        handler.indekser(ytelser);

        verify(persistentOppdatering, times(1)).lagreBrukeroppdateringerIDB(anyList());
        verify(persistentOppdatering).lagreBrukeroppdateringerIDB(captor.capture());

        List<BrukerinformasjonFraFil> oppdateringer = captor.getValue();
        assertThat(oppdateringer).hasSize(2);


        assertThat(oppdateringer.get(0).getUtlopsdatoFasett()).isNull();
        assertThat(oppdateringer.get(1).getUtlopsdatoFasett()).isNotNull();
    }

    @Test
    public void leggerTilAAPMAXTid() {
        LoependeYtelser ytelser = lagLoependeYtelser(asList(
                lagVedtak("10108000397", "AA", "AAP"), //TESTFAMILIE
                lagVedtak("10108000399", "AA", "AAP", new BigInteger("205"), BigInteger.TEN) //TESTFAMILIE
        ));

        handler.indekser(ytelser);

        verify(persistentOppdatering, times(1)).lagreBrukeroppdateringerIDB(anyList());
        verify(persistentOppdatering).lagreBrukeroppdateringerIDB(captor.capture());

        List<BrukerinformasjonFraFil> oppdateringer = captor.getValue();
        assertThat(oppdateringer).hasSize(2);

        assertThat(oppdateringer.get(0).getPersonid()).isEqualTo("anotherPersonId");
        assertThat(oppdateringer.get(1).getPersonid()).isEqualTo("anotherPersonId");
        assertThat(oppdateringer.get(0).getYtelse()).isEqualTo(YtelseMapping.AAP_MAXTID);
        assertThat(oppdateringer.get(1).getYtelse()).isEqualTo(YtelseMapping.AAP_MAXTID);
    }

    @Test
    public void leggerIkkeTilAAPMaxTilVedAAPUnntak() {
        LoependeVedtak vedtak = lagVedtak("10108000397", "AA", "AAP"); //TESTFAMILIE
        vedtak.getAaptellere().setAntallDagerUnntak(BigInteger.ONE);
        LoependeYtelser ytelser = lagLoependeYtelser(asList(vedtak));

        handler.indekser(ytelser);

        verify(persistentOppdatering, times(1)).lagreBrukeroppdateringerIDB(anyList());
        verify(persistentOppdatering).lagreBrukeroppdateringerIDB(captor.capture());

        List<BrukerinformasjonFraFil> oppdateringer = captor.getValue();
        assertThat(oppdateringer).hasSize(1);

        assertThat(oppdateringer.get(0).getPersonid()).isNotNull();
        assertThat(oppdateringer.get(0).getAapmaxtidUke()).isNull();
        assertThat(oppdateringer.get(0).getAapmaxtidUkeFasett()).isNull();
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