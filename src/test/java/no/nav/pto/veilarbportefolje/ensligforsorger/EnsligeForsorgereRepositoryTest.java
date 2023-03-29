package no.nav.pto.veilarbportefolje.ensligforsorger;

import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.EnsligeForsorgerOvergangsstønadTiltak;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.*;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import static no.nav.pto.veilarbportefolje.ensligforsorger.domain.Aktivitetstype.BARN_UNDER_ETT_ÅR;
import static no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.Periodetype.NY_PERIODE_FOR_NYTT_BARN;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;


@SpringBootTest(classes = ApplicationConfigTest.class)
public class EnsligeForsorgereRepositoryTest {

    @Autowired
    private EnsligeForsorgereRepository ensligeForsorgereRepository;

    @Autowired
    private JdbcTemplate postgres;

    @BeforeEach
    public void setup() {
        postgres.update("truncate TABLE enslige_forsorgere CASCADE");
    }

    @Test
    public void testYoungestDate() {
        LocalDate date1 = LocalDate.of(2022, 3, 22);
        LocalDate date2 = LocalDate.of(2022, 5, 18);
        LocalDate date3 = LocalDate.of(2021, 2, 6);

        LocalDate newestDate = Stream.of(date1, date2, date3).max(LocalDate::compareTo).get();
        Assert.assertEquals(newestDate, date2);
    }

    //Få inn melding om opphørt vedtak
    @Test
    public void lagreOgHenteOvergangsstønadVedMotattKafkamelding() {
        List<Barn> barn = List.of(new Barn("11032245678", null), new Barn(null, LocalDate.of(2023, 5, 4)));
        List<Periode> periodeType = List.of(new Periode(LocalDate.now().minusDays(10), LocalDate.now().plusDays(20), NY_PERIODE_FOR_NYTT_BARN, Aktivitetstype.BARN_UNDER_ETT_ÅR));
        VedtakOvergangsstønadArbeidsoppfølging melding = new VedtakOvergangsstønadArbeidsoppfølging(
                54321L,
                "12345678910",
                barn,
                Stønadstype.OVERGANGSSTØNAD,
                periodeType,
                Vedtaksresultat.INNVILGET

        );

        ensligeForsorgereRepository.lagreOvergangsstonad(melding);
        Optional<EnsligeForsorgerOvergangsstønadTiltak> ensligeForsorgerOvergangsstønadTiltakOptional = ensligeForsorgereRepository.hentOvergangsstønadForEnsligeForsorger(melding.personIdent(), true);
        Optional<LocalDate> yngsteBarnFdato = ensligeForsorgereRepository.hentYngsteBarn(melding.vedtakId());

        Assert.assertTrue(ensligeForsorgerOvergangsstønadTiltakOptional.isPresent());
        Assert.assertEquals(54321L, ensligeForsorgerOvergangsstønadTiltakOptional.get().vedtakid().longValue());
        Assert.assertEquals(BARN_UNDER_ETT_ÅR, ensligeForsorgerOvergangsstønadTiltakOptional.get().aktivitetsType());

        Assert.assertTrue(yngsteBarnFdato.isPresent());
        Assert.assertEquals(LocalDate.of(2023, 5, 4), yngsteBarnFdato.get());
    }

    @Test
    public void oppdatereVedtakVedMeldingOmBarnetsFnr() {
        lagreInitiellVedtakIdatabase();

        List<Barn> barn = List.of(new Barn("11032245678", null), new Barn("14052312320", null));
        List<Periode> periodeType = List.of(new Periode(LocalDate.of(2023, 4, 4), LocalDate.of(2024, 4, 4), NY_PERIODE_FOR_NYTT_BARN, Aktivitetstype.BARN_UNDER_ETT_ÅR));
        VedtakOvergangsstønadArbeidsoppfølging melding = new VedtakOvergangsstønadArbeidsoppfølging(
                12345L,
                "12345678910",
                barn,
                Stønadstype.OVERGANGSSTØNAD,
                periodeType,
                Vedtaksresultat.INNVILGET

        );

        ensligeForsorgereRepository.lagreOvergangsstonad(melding);

        Optional<LocalDate> yngsteBarnFdato = ensligeForsorgereRepository.hentYngsteBarn(melding.vedtakId());

        Assert.assertTrue(yngsteBarnFdato.isPresent());
        Assert.assertEquals(LocalDate.of(2023, 5, 14), yngsteBarnFdato.get());

    }

    @Test
    public void oppdatereVedtakVedMeldingOmOpphortVedtak() {
        lagreInitiellVedtakIdatabase();

        List<Barn> barn = List.of(new Barn("11032245678", null), new Barn("14052312320", null));
        List<Periode> periodeType = List.of(new Periode(LocalDate.of(2023, 4, 4), LocalDate.of(2024, 4, 4), NY_PERIODE_FOR_NYTT_BARN, Aktivitetstype.BARN_UNDER_ETT_ÅR));
        VedtakOvergangsstønadArbeidsoppfølging melding = new VedtakOvergangsstønadArbeidsoppfølging(
                12345L,
                "12345678910",
                barn,
                Stønadstype.OVERGANGSSTØNAD,
                periodeType,
                Vedtaksresultat.OPPHØRT

        );

        ensligeForsorgereRepository.lagreOvergangsstonad(melding);
        Optional<EnsligeForsorgerOvergangsstønadTiltak> ensligeForsorgerOvergangsstønadTiltakOptional = ensligeForsorgereRepository.hentOvergangsstønadForEnsligeForsorger(melding.personIdent(), true);

        Assert.assertTrue(ensligeForsorgerOvergangsstønadTiltakOptional.isEmpty());

    }


    private void lagreInitiellVedtakIdatabase() {
        List<Barn> barn = List.of(new Barn("11032245678", null), new Barn(null, LocalDate.of(2023, 5, 4)));
        List<Periode> periodeType = List.of(new Periode(LocalDate.of(2023, 4, 4), LocalDate.of(2024, 4, 4), NY_PERIODE_FOR_NYTT_BARN, Aktivitetstype.BARN_UNDER_ETT_ÅR));
        VedtakOvergangsstønadArbeidsoppfølging melding = new VedtakOvergangsstønadArbeidsoppfølging(
                12345L,
                "12345678910",
                barn,
                Stønadstype.OVERGANGSSTØNAD,
                periodeType,
                Vedtaksresultat.INNVILGET

        );

        ensligeForsorgereRepository.lagreOvergangsstonad(melding);
    }

    private void lagreRandomVedtakIdatabase(Long vedtakId, Fnr fnr, LocalDate vedtakPeriodeFra, LocalDate vedtakPeriodeTil) {
        List<Barn> barn = List.of(new Barn(randomFnr().toString(), null));
        List<Periode> periodeType = List.of(new Periode(vedtakPeriodeFra, vedtakPeriodeTil, NY_PERIODE_FOR_NYTT_BARN, Aktivitetstype.BARN_UNDER_ETT_ÅR));
        VedtakOvergangsstønadArbeidsoppfølging melding = new VedtakOvergangsstønadArbeidsoppfølging(
                vedtakId,
                fnr.toString(),
                barn,
                Stønadstype.OVERGANGSSTØNAD,
                periodeType,
                Vedtaksresultat.INNVILGET

        );

        ensligeForsorgereRepository.lagreOvergangsstonad(melding);
    }


    @Test
    void testHentOvergangsstønadForEnsligeForsorgere() {
        List<Long> vedtakIds = List.of(new Random().nextLong(10000l), new Random().nextLong(10000l), new Random().nextLong(10000l),
                new Random().nextLong(10000l), new Random().nextLong(10000l));
        List<Fnr> fnrList = List.of(Fnr.of("11018012321"), Fnr.of("12018012321"), Fnr.of("13018012321"),
                Fnr.of("14018012321"), Fnr.of("15018012321"));
        lagreRandomVedtakIdatabase(vedtakIds.get(0), fnrList.get(0), LocalDate.now().plusMonths(4), LocalDate.now().plusMonths(10));
        lagreRandomVedtakIdatabase(vedtakIds.get(1), fnrList.get(1), LocalDate.now().plusDays(3), LocalDate.now().plusMonths(1));
        lagreRandomVedtakIdatabase(vedtakIds.get(2), fnrList.get(2), LocalDate.now().minusDays(20), LocalDate.now().plusMonths(1));
        lagreRandomVedtakIdatabase(vedtakIds.get(3), fnrList.get(3), LocalDate.now().minusDays(30), LocalDate.now().minusDays(2));
        lagreRandomVedtakIdatabase(vedtakIds.get(4), fnrList.get(4), LocalDate.now().minusDays(13), LocalDate.now().plusMonths(1));

        List<EnsligeForsorgerOvergangsstønadTiltak> ensligeForsorgerOvergangsstønadTiltaks = ensligeForsorgereRepository.hentOvergangsstønadForEnsligeForsorger(fnrList, true);
        Assert.assertEquals(ensligeForsorgerOvergangsstønadTiltaks.size(), 4);
        Assert.assertTrue(ensligeForsorgerOvergangsstønadTiltaks.stream().anyMatch(x -> x.personIdent().equals(fnrList.get(0))));
        Assert.assertTrue(ensligeForsorgerOvergangsstønadTiltaks.stream().anyMatch(x -> x.personIdent().equals(fnrList.get(1))));
        Assert.assertTrue(ensligeForsorgerOvergangsstønadTiltaks.stream().anyMatch(x -> x.personIdent().equals(fnrList.get(2))));
        Assert.assertTrue(ensligeForsorgerOvergangsstønadTiltaks.stream().anyMatch(x -> x.personIdent().equals(fnrList.get(4))));

        ensligeForsorgerOvergangsstønadTiltaks = ensligeForsorgereRepository.hentOvergangsstønadForEnsligeForsorger(fnrList, false);
        Assert.assertEquals(ensligeForsorgerOvergangsstønadTiltaks.size(), 2);
        Assert.assertTrue(ensligeForsorgerOvergangsstønadTiltaks.stream().anyMatch(x -> x.personIdent().equals(fnrList.get(2))));
        Assert.assertTrue(ensligeForsorgerOvergangsstønadTiltaks.stream().anyMatch(x -> x.personIdent().equals(fnrList.get(4))));
    }
}
