package no.nav.pto.veilarbportefolje.registrering.endring

import no.nav.common.types.identer.AktorId
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.TestDataUtils
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.ZonedDateTime


class EndringIRegistreringServiceTest : EndToEndTest() {

    @Autowired
    lateinit var endringIRegistreringService: EndringIRegistreringService

    @Test
    fun behandleKafkaMeldingLogikk() {
        val aktorId = TestDataUtils.randomAktorId()
        testDataClient.lagreBrukerUnderOppfolging(aktorId, ZonedDateTime.now())
        val kafkaRegistreringMelding = getArbeidssokerBesvarelseEvent(aktorId.get())
        endringIRegistreringService.behandleKafkaMeldingLogikk(kafkaRegistreringMelding)

        val getResponse = opensearchTestClient.fetchDocument(aktorId)
        AssertionsForClassTypes.assertThat(getResponse.isExists).isTrue

        val endretSituasjon: String = getResponse.sourceAsMap["brukers_situasjon"] as String
        val endretSituasjonSistEndret: String  = getResponse.sourceAsMap["brukers_situasjon_sist_endret"] as String

        AssertionsForClassTypes.assertThat(endretSituasjon).isEqualTo(kafkaRegistreringMelding.besvarelse.dinSituasjon.verdi.toString())
        AssertionsForClassTypes.assertThat(endretSituasjonSistEndret).isEqualTo(kafkaRegistreringMelding.besvarelse.dinSituasjon.endretTidspunkt.toString())


    }
}
