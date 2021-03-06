package no.nav.pto.veilarbportefolje.arenafiler;

import no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser.IndekserYtelserHandler;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.database.PersistentOppdatering;
import no.nav.pto.veilarbportefolje.domene.BrukerinformasjonFraFil;
import no.nav.pto.veilarbportefolje.domene.YtelseMapping;
import no.nav.melding.virksomhet.loependeytelser.v1.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
import static org.mockito.ArgumentMatchers.anyCollection;
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

        handler.lagreYtelser(ytelser);

        verify(persistentOppdatering, times(1)).lagreBrukeroppdateringerIDB(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    public void leggerTilManedFasetteringOmUtlopsdatoErInnenforSammeKalenderar() throws Exception {
        LoependeYtelser ytelser = lagLoependeYtelser(asList(
                lagVedtak("10108000397", new BigInteger("53"), BigInteger.TEN), //TESTFAMILIE // Utenfor kalenderåret
                lagVedtak("10108000399", "AA", "AAP") //TESTFAMILIE
        ));

        handler.lagreYtelser(ytelser);

        verify(persistentOppdatering, times(1)).lagreBrukeroppdateringerIDB(captor.capture());

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

        handler.lagreYtelser(ytelser);

        verify(persistentOppdatering, times(1)).lagreBrukeroppdateringerIDB(captor.capture());

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
        vedtak.getAaptellere().setAntallDagerIgjenUnntak(BigInteger.ONE);
        LoependeYtelser ytelser = lagLoependeYtelser(asList(vedtak));

        handler.lagreYtelser(ytelser);

        verify(persistentOppdatering, times(1)).lagreBrukeroppdateringerIDB(captor.capture());

        List<BrukerinformasjonFraFil> oppdateringer = captor.getValue();
        assertThat(oppdateringer).hasSize(1);

        assertThat(oppdateringer.get(0).getAapUnntakDagerIgjen().intValue()).isEqualTo(1);
        assertThat(oppdateringer.get(0).getPersonid()).isNotNull();
        assertThat(oppdateringer.get(0).getAapmaxtidUke()).isNull();
        assertThat(oppdateringer.get(0).getAapmaxtidUkeFasett()).isNull();
    }

    @Test
    public void lagreYtelserMedNullVerdiPaaAAPForAaTesteTransisjonsXSD() {
        LoependeVedtak vedtak = lagVedtakMedNullverdiAAP("10108000397");
        LoependeYtelser ytelser = lagLoependeYtelser(asList(vedtak));

        handler.lagreYtelser(ytelser);

        verify(persistentOppdatering, times(1)).lagreBrukeroppdateringerIDB(captor.capture());

        List<BrukerinformasjonFraFil> oppdateringer = captor.getValue();
        assertThat(oppdateringer).hasSize(1);

        assertThat(captor.getValue()).hasSize(1);
        assertThat(oppdateringer.get(0).getPersonid()).isNotNull();
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

    private LoependeVedtak lagVedtakMedNullverdiAAP(String fnr) {
        LoependeVedtak vedtak = new LoependeVedtak();
        AAPtellere aapTellere = new AAPtellere();

        vedtak.setPersonident(fnr);

        aapTellere.setAntallUkerIgjen(BigInteger.ONE);
        aapTellere.setAntallDagerIgjen(BigInteger.ONE);
        aapTellere.setAntallDagerIgjenUnntak(null);

        vedtak.setAaptellere(aapTellere);

        vedtak.setSakstypeKode("AA");
        vedtak.setRettighetstypeKode("AAP");

        settVedtaksperiode(vedtak);

        return vedtak;
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

            settVedtaksperiode(vedtak);
        }

        return vedtak;
    }

    private void settVedtaksperiode(LoependeVedtak vedtak) {
        try {
            Periode periode = new Periode();
            LocalDate now = LocalDate.now().plusDays(10);
            GregorianCalendar gcal = GregorianCalendar.from(now.atStartOfDay(ZoneId.of("Europe/Oslo")));
            XMLGregorianCalendar xcal = null;
            xcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);

            periode.setTom(xcal);
            vedtak.setVedtaksperiode(periode);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
