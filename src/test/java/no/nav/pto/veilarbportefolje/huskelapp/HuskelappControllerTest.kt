package no.nav.pto.veilarbportefolje.huskelapp

import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.client.Decision.Permit
import no.nav.pto.veilarbportefolje.auth.AuthService
import no.nav.pto.veilarbportefolje.auth.AuthUtils
import no.nav.pto.veilarbportefolje.auth.PoaoTilgangWrapper
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest
import no.nav.pto.veilarbportefolje.domene.value.NavKontor
import no.nav.pto.veilarbportefolje.huskelapp.controller.HuskelappController
import no.nav.pto.veilarbportefolje.huskelapp.controller.dto.*
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2
import no.nav.pto.veilarbportefolje.util.TestDataClient
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest

import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDate
import java.util.*

@WebMvcTest(controllers = [HuskelappController::class])
@Import(
    ApplicationConfigTest::class
)
open class HuskelappControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    protected var testDataClient: TestDataClient? = null

    @MockitoBean
    private val authService: AuthService? = null

    @MockitoBean
    private val poaoTilgangWrapper: PoaoTilgangWrapper? = null

    @MockitoBean
    private val brukerService: BrukerServiceV2? = null

    @Autowired
    private val pdlIdentRepository: PdlIdentRepository? = null

    @Test
    @Throws(Exception::class)
    fun test_opprett_og_hent_huskelapp_for_bruker() {
        val fnr = Fnr.of("10987654321")
        val enhetId = EnhetId.of("1234")
        val aktorId = AktorId.of("99988877766655")
        val veilederId = AuthUtils.getInnloggetVeilederIdent()
        val huskelappfrist = LocalDate.now()
        testDataClient!!.lagreBrukerUnderOppfolging(aktorId, fnr, NavKontor.of(enhetId.get()), veilederId)

        Mockito.`when`(authService!!.harVeilederTilgangTilEnhet(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(true)
        Mockito.`when`(brukerService!!.hentVeilederForBruker(aktorId)).thenReturn(Optional.of(veilederId))
        Mockito.`when`(brukerService.hentAktorId(fnr)).thenReturn(Optional.of(aktorId))
        Mockito.`when`(brukerService.hentNavKontor(fnr)).thenReturn(Optional.of(NavKontor.of(enhetId.get())))

        val opprettRequest = HuskelappOpprettRequest(fnr, huskelappfrist, "Test")
        val opprettetHuskelappId = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/huskelapp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(opprettRequest))
            )
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andReturn()
            .response
            .contentAsString

        val hentForBrukerRequest = HuskelappForBrukerRequest(fnr, enhetId)
        val huskelappId = JsonUtils.fromJson(
            opprettetHuskelappId,
            HuskelappOpprettResponse::class.java
        )
        val expected = HuskelappResponse(
            huskelappId.huskelappId,
            fnr,
            enhetId,
            huskelappfrist,
            "Test",
            LocalDate.now(),
            veilederId.value
        )

        val hentHuskelappResult = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/hent-huskelapp-for-bruker")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(hentForBrukerRequest))
                    .header("test_ident", "Z12345")
                    .header("test_ident_type", "INTERN")
            )
            .andExpect(MockMvcResultMatchers.status().`is`(200))
            .andReturn().response.contentAsString

        val hentetHuskelappBody = JsonUtils.fromJson(
            hentHuskelappResult,
            HuskelappResponse::class.java
        )
        Assertions.assertThat(hentetHuskelappBody.huskelappId).isEqualTo(expected.huskelappId)
        Assertions.assertThat(hentetHuskelappBody.brukerFnr).isEqualTo(expected.brukerFnr)
        Assertions.assertThat(hentetHuskelappBody.enhetId).isEqualTo(expected.enhetId)
        Assertions.assertThat(hentetHuskelappBody.frist).isEqualTo(expected.frist)
        Assertions.assertThat(hentetHuskelappBody.kommentar).isEqualTo(expected.kommentar)
        Assertions.assertThat(hentetHuskelappBody.endretDato).isEqualTo(expected.endretDato)
        Assertions.assertThat(hentetHuskelappBody.endretAv).isEqualTo(expected.endretAv)
    }

    @Test
    @Throws(Exception::class)
    fun test_at_vi_redigere_huskelapp() {
        val fnr = Fnr.of("12345678910")
        val enhetId = EnhetId.of("1234")
        val aktorId = AktorId.of("1223234234234")
        val veilederId = AuthUtils.getInnloggetVeilederIdent()
        val opprettRequest = HuskelappOpprettRequest(fnr, LocalDate.now(), "Test")
        testDataClient!!.lagreBrukerUnderOppfolging(aktorId, fnr, NavKontor.of(enhetId.get()), veilederId)

        Mockito.`when`(poaoTilgangWrapper!!.harVeilederTilgangTilModia()).thenReturn(Permit)
        Mockito.`when`(poaoTilgangWrapper.harTilgangTilPerson(ArgumentMatchers.any())).thenReturn(Permit)
        Mockito.`when`(authService!!.harVeilederTilgangTilEnhet(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(true)
        Mockito.`when`(brukerService!!.hentVeilederForBruker(aktorId)).thenReturn(Optional.of(veilederId))
        Mockito.`when`(brukerService.hentAktorId(fnr)).thenReturn(Optional.of(aktorId))
        Mockito.`when`(brukerService.hentNavKontor(fnr)).thenReturn(Optional.of(NavKontor.of(enhetId.get())))

        val identer = listOf(
            PDLIdent(aktorId.get(), false, Gruppe.AKTORID),
            PDLIdent(fnr.get(), false, Gruppe.FOLKEREGISTERIDENT)
        )
        pdlIdentRepository!!.upsertIdenter(identer)

        val opprettetHuskelappIdbody = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/huskelapp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(opprettRequest))
            ).andReturn().response.contentAsString
        val huskelappIdBody = JsonUtils.fromJson(
            opprettetHuskelappIdbody,
            HuskelappOpprettResponse::class.java
        )
        val hentForBrukerForRedigeringRequest = HuskelappForBrukerRequest(fnr, enhetId)
        val hentHuskelappForRedigeringResult = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/hent-huskelapp-for-bruker")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(hentForBrukerForRedigeringRequest))
                    .header("test_ident", "Z12345")
                    .header("test_ident_type", "INTERN")
            )
            .andExpect(MockMvcResultMatchers.status().`is`(200))
            .andReturn().response.contentAsString

        val hentetHuskelappBody = JsonUtils.fromJson(
            hentHuskelappForRedigeringResult,
            HuskelappResponse::class.java
        )
        Assertions.assertThat(hentetHuskelappBody.huskelappId).isEqualTo(huskelappIdBody.huskelappId)
        Assertions.assertThat(hentetHuskelappBody.frist).isEqualTo(opprettRequest.frist)
        Assertions.assertThat(hentetHuskelappBody.kommentar).isEqualTo(opprettRequest.kommentar)
        Assertions.assertThat(hentetHuskelappBody.endretAv).isEqualTo(veilederId.value)

        val redigereRequest = HuskelappRedigerRequest(
            UUID.fromString(huskelappIdBody.huskelappId),
            fnr,
            null,
            "Test at det blir en ny kommentar"
        )
        mockMvc
            .perform(
                MockMvcRequestBuilders.put("/api/v1/huskelapp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(redigereRequest))
                    .header("test_ident", "Z12345")
                    .header("test_ident_type", "INTERN")
            ).andExpect(MockMvcResultMatchers.status().`is`(204))

        val hentForBrukerEtterRedigeringRequest = HuskelappForBrukerRequest(fnr, enhetId)
        val hentHuskelappEtterRedigeringResult = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/hent-huskelapp-for-bruker")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(hentForBrukerEtterRedigeringRequest))
                    .header("test_ident", "Z12345")
                    .header("test_ident_type", "INTERN")
            )
            .andExpect(MockMvcResultMatchers.status().`is`(200))
            .andReturn().response.contentAsString

        val hentetHuskelappEtterRedigeringBody = JsonUtils.fromJson(
            hentHuskelappEtterRedigeringResult,
            HuskelappResponse::class.java
        )
        Assertions.assertThat(hentetHuskelappEtterRedigeringBody.huskelappId).isEqualTo(huskelappIdBody.huskelappId)
        Assertions.assertThat(hentetHuskelappEtterRedigeringBody.frist).isEqualTo(redigereRequest.frist)
        Assertions.assertThat(hentetHuskelappEtterRedigeringBody.kommentar).isEqualTo(redigereRequest.kommentar)
        Assertions.assertThat(hentetHuskelappEtterRedigeringBody.endretAv).isEqualTo(veilederId.value)
    }

    @Test
    @Throws(Exception::class)
    fun test_opprett_huskelapp_uten_kommentar_og_frist() {
        val fnr = Fnr.of("76543218457")
        val enhetId = EnhetId.of("1234")
        val aktorId = AktorId.of("1223234234234")
        val veilederId = AuthUtils.getInnloggetVeilederIdent()
        val opprettRequest = HuskelappOpprettRequest(fnr, null, null)
        testDataClient!!.lagreBrukerUnderOppfolging(aktorId, fnr, NavKontor.of(enhetId.get()), veilederId)

        Mockito.`when`(authService!!.harVeilederTilgangTilEnhet(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(true)
        Mockito.`when`(brukerService!!.hentVeilederForBruker(aktorId)).thenReturn(Optional.of(veilederId))
        Mockito.`when`(brukerService.hentAktorId(fnr)).thenReturn(Optional.of(aktorId))
        Mockito.`when`(brukerService.hentNavKontor(fnr)).thenReturn(Optional.of(NavKontor.of(enhetId.get())))

        val identer = listOf(
            PDLIdent(aktorId.get(), false, Gruppe.AKTORID),
            PDLIdent(fnr.get(), false, Gruppe.FOLKEREGISTERIDENT)
        )
        pdlIdentRepository!!.upsertIdenter(identer)

        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/huskelapp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(opprettRequest))
                    .header("test_ident", "Z12345")
                    .header("test_ident_type", "INTERN")
            )
            .andExpect(MockMvcResultMatchers.status().`is`(400))
            .andReturn().response.contentAsString
    }

    @Test
    @Throws(Exception::class)
    fun test_slett_huskelapp() {
        val fnr = Fnr.of("34567823456")
        val enhetId = EnhetId.of("1234")
        val aktorId = AktorId.of("1223234234234")
        val opprettRequest = HuskelappOpprettRequest(fnr, LocalDate.now(), "Test")

        Mockito.`when`(poaoTilgangWrapper!!.harVeilederTilgangTilModia()).thenReturn(Permit)
        Mockito.`when`(poaoTilgangWrapper.harTilgangTilPerson(ArgumentMatchers.any())).thenReturn(Permit)
        Mockito.`when`(authService!!.harVeilederTilgangTilEnhet(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(true)
        Mockito.`when`(brukerService!!.hentVeilederForBruker(aktorId))
            .thenReturn(Optional.of(AuthUtils.getInnloggetVeilederIdent()))
        Mockito.`when`(brukerService.hentAktorId(fnr)).thenReturn(Optional.of(aktorId))
        Mockito.`when`(brukerService.hentNavKontor(fnr)).thenReturn(Optional.of(NavKontor.of(enhetId.get())))

        val identer = listOf(
            PDLIdent(aktorId.get(), false, Gruppe.AKTORID),
            PDLIdent(fnr.get(), false, Gruppe.FOLKEREGISTERIDENT)
        )
        pdlIdentRepository!!.upsertIdenter(identer)

        val opprettetBody = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/huskelapp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(opprettRequest))
                    .header("test_ident", "Z12345")
                    .header("test_ident_type", "INTERN")
            )
            .andExpect(MockMvcResultMatchers.status().`is`(201))
            .andReturn().response.contentAsString
        val opprettetResponse = JsonUtils.fromJson(
            opprettetBody,
            HuskelappOpprettResponse::class.java
        )

        val slettRequest = HuskelappSlettRequest(opprettetResponse.huskelappId)
        mockMvc
            .perform(
                MockMvcRequestBuilders.delete("/api/v1/huskelapp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(slettRequest))
                    .header("test_ident", "Z12345")
                    .header("test_ident_type", "INTERN")
            )
            .andExpect(MockMvcResultMatchers.status().`is`(204))

        val hentForBrukerRequest = HuskelappForBrukerRequest(fnr, enhetId)
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/hent-huskelapp-for-bruker")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(hentForBrukerRequest))
                    .header("test_ident", "Z12345")
                    .header("test_ident_type", "INTERN")
            )
            .andExpect(MockMvcResultMatchers.status().`is`(200))
            .andExpect(MockMvcResultMatchers.content().string(""))
    }
}
