package no.nav.pto.veilarbportefolje.oppfolging

import no.nav.pto.veilarbportefolje.oppfolging.dto.VeilederTilordnetDTO
import no.nav.pto.veilarbportefolje.util.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Stream

class VeilederTilordnetDTOKontraktTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("gyldigeMeldinger")
    fun `skal deserialisere gyldig melding uten NPE`(filnavn: String, json: String) {
        val dto = objectMapper.readValue(json, VeilederTilordnetDTO::class.java)

        assertThat(dto.aktorId).isNotNull()
        // Tilgang til nullable felter skal ikke kaste NPE uavhengig av verdi
        dto.veilederId?.value
        dto.tilordnetTidspunkt
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ugyldigeMeldinger")
    fun `skal feile deserialisering ved ugyldig payload`(filnavn: String, json: String) {
        assertThatThrownBy { objectMapper.readValue(json, VeilederTilordnetDTO::class.java) }
            .isInstanceOf(RuntimeException::class.java)
    }

    companion object {
        @JvmStatic
        fun gyldigeMeldinger(): Stream<Arguments> =
            lastJsonFilerFraKatalog("/kafka/veileder-tilordnet/gyldig")

        @JvmStatic
        fun ugyldigeMeldinger(): Stream<Arguments> =
            lastJsonFilerFraKatalog("/kafka/veileder-tilordnet/ugyldig")

        private fun lastJsonFilerFraKatalog(katalogsti: String): Stream<Arguments> {
            val uri: URI = VeilederTilordnetDTOKontraktTest::class.java.getResource(katalogsti)?.toURI()
                ?: return Stream.empty()
            return Files.walk(Paths.get(uri), 1)
                .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
                .sorted()
                .map { fil -> Arguments.of(fil.fileName.toString(), Files.readString(fil)) }
        }
    }
}
