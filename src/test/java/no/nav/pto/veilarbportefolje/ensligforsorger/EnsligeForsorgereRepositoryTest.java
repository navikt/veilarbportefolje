package no.nav.pto.veilarbportefolje.ensligforsorger;

import no.nav.familie.eksterne.kontrakter.arbeidsoppfolging.Aktivitetstype;
import no.nav.familie.eksterne.kontrakter.arbeidsoppfolging.Barn;
import no.nav.familie.eksterne.kontrakter.arbeidsoppfolging.Periode;
import no.nav.familie.eksterne.kontrakter.arbeidsoppfolging.Stønadstype;
import no.nav.familie.eksterne.kontrakter.arbeidsoppfolging.VedtakOvergangsstønadArbeidsoppfølging;
import no.nav.familie.eksterne.kontrakter.arbeidsoppfolging.Vedtaksresultat;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.EnsligeForsorgerOvergangsstønadTiltak;
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.Periodetype;
import org.junit.Assert;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static no.nav.pto.veilarbportefolje.ensligforsorger.domain.Periodetype.NY_PERIODE_FOR_NYTT_BARN;


@SpringBootTest(classes = ApplicationConfigTest.class)
public class EnsligeForsorgereRepositoryTest {

	@Autowired
	private EnsligeForsorgereRepository ensligeForsorgereRepository;

	@Test
	public void test() {
		Integer test = ensligeForsorgereRepository.lagreStonadstype("Test");
		Integer test1 = ensligeForsorgereRepository.lagreStonadstype("Test1");
		Integer test2 = ensligeForsorgereRepository.lagreStonadstype("Test2");
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
		List<Periode> periodeType = List.of(new Periode(LocalDate.of(2023, 4, 4), LocalDate.of(2024, 4, 4), no.nav.familie.eksterne.kontrakter.arbeidsoppfolging.Periodetype.NY_PERIODE_FOR_NYTT_BARN, Aktivitetstype.BARN_UNDER_ETT_ÅR));
		VedtakOvergangsstønadArbeidsoppfølging melding = new VedtakOvergangsstønadArbeidsoppfølging(
				54321L,
				"12345678910",
				barn,
				Stønadstype.OVERGANGSSTØNAD,
				periodeType,
				Vedtaksresultat.INNVILGET

		);

		ensligeForsorgereRepository.lagreEnsligeForsorgereStonad(melding);
		Optional<EnsligeForsorgerOvergangsstønadTiltak> ensligeForsorgerOvergangsstønadTiltakOptional = ensligeForsorgereRepository.hentOvergangsstønadForEnsligeForsorger(melding.getPersonIdent());
		Optional<LocalDate> yngsteBarnFdato = ensligeForsorgereRepository.hentYngsteBarn(melding.getVedtakId());

		Assert.assertTrue(ensligeForsorgerOvergangsstønadTiltakOptional.isPresent());
		Assert.assertEquals(54321L, ensligeForsorgerOvergangsstønadTiltakOptional.get().vedtakid().longValue());
		Assert.assertEquals("BARN_UNDER_ETT_ÅR", ensligeForsorgerOvergangsstønadTiltakOptional.get().aktivitetsType());

		Assert.assertTrue(yngsteBarnFdato.isPresent());
		Assert.assertEquals(LocalDate.of(2023, 5, 4), yngsteBarnFdato.get());
	}

	@Test
	public void oppdatereVedtakVedMeldingOmBarnetsFnr() {
		lagreInitiellVedtakIdatabase();

		List<Barn> barn = List.of(new Barn("11032245678", null), new Barn("14052312320", null));
		List<Periode> periodeType = List.of(new Periode(LocalDate.of(2023, 4, 4), LocalDate.of(2024, 4, 4), no.nav.familie.eksterne.kontrakter.arbeidsoppfolging.Periodetype.NY_PERIODE_FOR_NYTT_BARN, Aktivitetstype.BARN_UNDER_ETT_ÅR));
		VedtakOvergangsstønadArbeidsoppfølging melding = new VedtakOvergangsstønadArbeidsoppfølging(
				12345L,
				"12345678910",
				barn,
				Stønadstype.OVERGANGSSTØNAD,
				periodeType,
				Vedtaksresultat.INNVILGET

		);

		ensligeForsorgereRepository.lagreEnsligeForsorgereStonad(melding);

		Optional<LocalDate> yngsteBarnFdato = ensligeForsorgereRepository.hentYngsteBarn(melding.getVedtakId());

		Assert.assertTrue(yngsteBarnFdato.isPresent());
		Assert.assertEquals(LocalDate.of(2023, 5, 14), yngsteBarnFdato.get());

	}

	@Test
	public void oppdatereVedtakVedMeldingOmOpphortVedtak() {
		lagreInitiellVedtakIdatabase();

		List<Barn> barn = List.of(new Barn("11032245678", null), new Barn("14052312320", null));
		List<Periode> periodeType = List.of(new Periode(LocalDate.of(2023, 4, 4), LocalDate.of(2024, 4, 4), no.nav.familie.eksterne.kontrakter.arbeidsoppfolging.Periodetype.NY_PERIODE_FOR_NYTT_BARN, Aktivitetstype.BARN_UNDER_ETT_ÅR));
		VedtakOvergangsstønadArbeidsoppfølging melding = new VedtakOvergangsstønadArbeidsoppfølging(
				12345L,
				"12345678910",
				barn,
				Stønadstype.OVERGANGSSTØNAD,
				periodeType,
				Vedtaksresultat.OPPHØRT

		);

		ensligeForsorgereRepository.lagreEnsligeForsorgereStonad(melding);
		Optional<EnsligeForsorgerOvergangsstønadTiltak> ensligeForsorgerOvergangsstønadTiltakOptional = ensligeForsorgereRepository.hentOvergangsstønadForEnsligeForsorger(melding.getPersonIdent());

		Assert.assertTrue(ensligeForsorgerOvergangsstønadTiltakOptional.isEmpty());

	}


	private void lagreInitiellVedtakIdatabase() {
		List<Barn> barn = List.of(new Barn("11032245678", null), new Barn(null, LocalDate.of(2023, 5, 4)));
		List<Periode> periodeType = List.of(new Periode(LocalDate.of(2023, 4, 4), LocalDate.of(2024, 4, 4), no.nav.familie.eksterne.kontrakter.arbeidsoppfolging.Periodetype.NY_PERIODE_FOR_NYTT_BARN, Aktivitetstype.BARN_UNDER_ETT_ÅR));
		VedtakOvergangsstønadArbeidsoppfølging melding = new VedtakOvergangsstønadArbeidsoppfølging(
				12345L,
				"12345678910",
				barn,
				Stønadstype.OVERGANGSSTØNAD,
				periodeType,
				Vedtaksresultat.INNVILGET

		);

		ensligeForsorgereRepository.lagreEnsligeForsorgereStonad(melding);
	}

}
