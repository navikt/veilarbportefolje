package no.nav.pto.veilarbportefolje.registrering.endring

import no.nav.common.types.identer.AktorId
import no.nav.common.utils.AssertUtils
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.util.TestDataUtils
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.time.ZoneId

@SpringBootTest(classes = [ApplicationConfigTest::class])
class EndringIRegistreringRepositoryTest {

    @Autowired
    lateinit var endringIRegistreringRepository: EndringIRegistreringRepository
    @Test
    fun upsertEndringIRegistrering() {
        val kafkaRegistreringMelding = getArbeidssokerBesvarelseEvent(AKTORID)
        endringIRegistreringRepository.upsertEndringIRegistrering(kafkaRegistreringMelding)

        val endringIRegistrering = endringIRegistreringRepository.hentBrukerEndringIRegistrering(AktorId.of(AKTORID))

        AssertUtils.assertTrue(endringIRegistrering.isPresent)
        AssertionsForClassTypes.assertThat(endringIRegistrering.get().aktorId).isEqualTo(AKTORID)
        AssertionsForClassTypes.assertThat(endringIRegistrering.get().brukersSituasjon)
            .isEqualTo(kafkaRegistreringMelding.besvarelse.dinSituasjon.verdi.toString())
        AssertionsForClassTypes.assertThat(endringIRegistrering.get().brukersSituasjonSistEndret).isEqualTo(
            LocalDate.ofInstant(
                kafkaRegistreringMelding.besvarelse.dinSituasjon.endretTidspunkt,
                ZoneId.systemDefault()
            )
        )
    }

    @Test
    fun upsertEndringIRegistrering_melding_mangler_din_situasjon() {
        val kafkaRegistreringMelding = getArbeidssokerBesvarelseEvent(AKTORID)
        kafkaRegistreringMelding.besvarelse.dinSituasjon = null

        endringIRegistreringRepository.upsertEndringIRegistrering(kafkaRegistreringMelding)

        val endringIRegistrering = endringIRegistreringRepository.hentBrukerEndringIRegistrering(AktorId.of(AKTORID))

        AssertUtils.assertTrue(endringIRegistrering.isPresent)
        AssertionsForClassTypes.assertThat(endringIRegistrering.get().aktorId).isEqualTo(AKTORID)
        AssertionsForClassTypes.assertThat(endringIRegistrering.get().brukersSituasjon)
            .isEqualTo(kafkaRegistreringMelding.besvarelse?.dinSituasjon?.verdi?.toString())
    }

    @Test
    fun slettEndringIRegistrering() {
        val kafkaRegistreringMelding = getArbeidssokerBesvarelseEvent(AKTORID)
        endringIRegistreringRepository.upsertEndringIRegistrering(kafkaRegistreringMelding)

        val endringIRegistrering = endringIRegistreringRepository.hentBrukerEndringIRegistrering(AktorId.of(AKTORID))
        AssertUtils.assertTrue(endringIRegistrering.isPresent)

        endringIRegistreringRepository.slettEndringIRegistrering(AktorId.of(AKTORID))
        val slettetEndringIRegistrering = endringIRegistreringRepository.hentBrukerEndringIRegistrering(
            AktorId.of(
                AKTORID
            )
        )

        AssertionsForClassTypes.assertThat(slettetEndringIRegistrering.isPresent).isFalse
    }

    @Test
    fun slettEndringIRegistrering_feiler_ikke_ved_sletting_av_bruker_som_ikke_har_endret() {
        val kafkaRegistreringMelding = getArbeidssokerBesvarelseEvent(AKTORID)
        endringIRegistreringRepository.upsertEndringIRegistrering(kafkaRegistreringMelding)

        val endringIRegistrering = endringIRegistreringRepository.hentBrukerEndringIRegistrering(AktorId.of(AKTORID))
        AssertUtils.assertTrue(endringIRegistrering.isPresent)

        endringIRegistreringRepository.slettEndringIRegistrering(AktorId.of("12345678910"))

        val slettetEndringIRegistrering = endringIRegistreringRepository.hentBrukerEndringIRegistrering(
            AktorId.of(
                AKTORID
            )
        )
        AssertionsForClassTypes.assertThat(slettetEndringIRegistrering.isPresent).isTrue
    }

    companion object {
        private val AKTORID = TestDataUtils.randomAktorId().get()
    }
}
