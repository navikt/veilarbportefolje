package no.nav.pto.veilarbportefolje.postgres

import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.NorskIdentMedHistorikk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime

class PostgresIdentPrioriteringTest {

    data class Data(val verdi: String, val sistEndret: LocalDateTime)

    @ParameterizedTest(name = "{0}")
    @MethodSource("prioriteringTestdata")
    fun `velgDataForPrioritertIdent skal velge riktig data`(
        beskrivelse: String,
        identer: List<NorskIdentMedHistorikk>,
        data: Map<String, Data>,
        forventetVerdi: String?
    ) {
        val resultat = velgDataForPrioritertIdent("testdata", AKTIV_FNR, identer, data) { it.sistEndret }

        assertThat(resultat?.verdi).isEqualTo(forventetVerdi)
    }

    @Test
    fun `alleIdenter skal returnere alle unike identer på tvers av brukere`() {
        val fnrIdenterPerBruker = mapOf(
            AKTIV_FNR to listOf(aktiv, historisk),
            ANNEN_BRUKER_FNR to listOf(NorskIdentMedHistorikk(ANNEN_BRUKER_FNR, false), historisk)
        )

        assertThat(alleIdenter(fnrIdenterPerBruker))
            .containsExactlyInAnyOrder(AKTIV_FNR, HISTORISK_FNR, ANNEN_BRUKER_FNR)
    }

    @Test
    fun `hentFnrIdenterPerBruker skal slå opp identer for brukere med fnr og returnere resultatet`() {
        val pdlIdentRepository: PdlIdentRepository = mock()
        val identerFraRepository = mapOf(
            AKTIV_FNR to listOf(aktiv, historisk),
            ANNEN_BRUKER_FNR to listOf(NorskIdentMedHistorikk(ANNEN_BRUKER_FNR, false))
        )
        `when`(pdlIdentRepository.hentFnrIdenterForBrukere(listOf(AKTIV_FNR, ANNEN_BRUKER_FNR)))
            .thenReturn(identerFraRepository)
        val brukere = listOf(
            PortefoljebrukerOpensearchModell(fnr = AKTIV_FNR),
            PortefoljebrukerOpensearchModell(fnr = null),
            PortefoljebrukerOpensearchModell(fnr = ANNEN_BRUKER_FNR)
        )

        val resultat = hentFnrIdenterPerBruker(pdlIdentRepository, brukere)

        verify(pdlIdentRepository).hentFnrIdenterForBrukere(listOf(AKTIV_FNR, ANNEN_BRUKER_FNR))
        assertThat(resultat).isEqualTo(identerFraRepository)
    }

    companion object {
        private const val AKTIV_FNR = "11111111111"
        private const val HISTORISK_FNR = "22222222222"
        private const val ANNET_HISTORISK_FNR = "33333333333"
        private const val ANNEN_BRUKER_FNR = "44444444444"

        private val aktiv = NorskIdentMedHistorikk(AKTIV_FNR, false)
        private val historisk = NorskIdentMedHistorikk(HISTORISK_FNR, true)
        private val annenHistorisk = NorskIdentMedHistorikk(ANNET_HISTORISK_FNR, true)

        private val naa = LocalDateTime.now()

        @JvmStatic
        fun prioriteringTestdata(): List<Arguments> = listOf(
            Arguments.of(
                "skal returnere null når ingen identer har data",
                listOf(aktiv, historisk),
                emptyMap<String, Data>(),
                null
            ),
            Arguments.of(
                "skal returnere null når bruker ikke har identer",
                emptyList<NorskIdentMedHistorikk>(),
                mapOf(AKTIV_FNR to Data("aktiv", naa)),
                null
            ),
            Arguments.of(
                "skal ignorere data på identer som ikke tilhører bruker",
                listOf(aktiv),
                mapOf(ANNEN_BRUKER_FNR to Data("annen bruker", naa)),
                null
            ),
            Arguments.of(
                "skal velge data på aktiv ident",
                listOf(aktiv, historisk),
                mapOf(AKTIV_FNR to Data("aktiv", naa)),
                "aktiv"
            ),
            Arguments.of(
                "skal velge data på historisk ident når aktiv ident mangler data",
                listOf(aktiv, historisk),
                mapOf(HISTORISK_FNR to Data("historisk", naa)),
                "historisk"
            ),
            Arguments.of(
                "skal prioritere aktiv ident selv om historisk ident er sist endret",
                listOf(aktiv, historisk),
                mapOf(
                    AKTIV_FNR to Data("aktiv", naa.minusDays(5)),
                    HISTORISK_FNR to Data("historisk", naa)
                ),
                "aktiv"
            ),
            Arguments.of(
                "skal velge sist endrede data når kun historiske identer har data",
                listOf(aktiv, historisk, annenHistorisk),
                mapOf(
                    HISTORISK_FNR to Data("eldst", naa.minusDays(5)),
                    ANNET_HISTORISK_FNR to Data("nyest", naa)
                ),
                "nyest"
            )
        )
    }
}
