package no.nav.pto.veilarbportefolje.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.utils.EnvironmentUtils
import no.nav.pto.veilarbportefolje.admin.dto.KafkaPartitionOffsetDto
import no.nav.pto.veilarbportefolje.admin.dto.KafkaResetOffsetRequest
import no.nav.pto.veilarbportefolje.auth.AuthUtils.hentApplikasjonFraContex
import no.nav.pto.veilarbportefolje.kafka.KafkaAdminService
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/admin/kafka")
@Tag(
    name = "Kafka Admin",
    description = "Admin-funksjonalitet for Kafka-operasjoner. Kun tilgjengelig for medlemmer av applikasjonens forvaltningsteam."
)
class KafkaAdminController(
    private val authContextHolder: AuthContextHolder,
    private val kafkaAdminService: KafkaAdminService
) {
    companion object {
        const val TEAM_OBO_PROD_GROUP_ID = "3bee8c85-8370-4a2e-a3a0-45632613a0ee"
        const val AZURE_TOKEN_GENERATOR_DEV = "dev-gcp:nais:azure-token-generator"
    }

    private val poaoAdmin =
        if (EnvironmentUtils.isProduction().orElse(false) ?: false) "prod-gcp:poao:poao-admin" else "dev-gcp:poao:poao-admin"

    @GetMapping("/topics")
    @Operation(
        summary = "Hent alle Kafka-topics applikasjonen konsumerer fra",
        description = "Returnerer en liste over alle Kafka-topics applikasjonen konsumerer fra."
    )
    fun hentKonsumerteKafkaTopics(): ResponseEntity<List<String>> {
        sjekkTilgangTilKafkaAdmin()
        return ResponseEntity.ok(kafkaAdminService.hentKonsumerteTopics())
    }

    @GetMapping("/offsets")
    @Operation(
        summary = "Hent siste commitede offset per partisjon for et topic",
        description = "Returnerer siste commitede offset per partisjon på tvers av applikasjonens consumer groups for angitt topic."
    )
    fun hentKafkaOffsets(@RequestParam topic: String): ResponseEntity<List<KafkaPartitionOffsetDto>> {
        sjekkTilgangTilKafkaAdmin()
        secureLog.info("Henter Kafka-offsets for topic=$topic (utførtAv=${hentNavIdentEllerUkjent()})")
        return ResponseEntity.ok(kafkaAdminService.hentCommitedeOffsets(topic))
    }

    @PostMapping("/resetOffset")
    @Operation(
        summary = "Resett Kafka-offsets for en consumer group og et topic",
        description = "Setter ny offset per partisjon for angitt consumer group og topic. Konsumenten må være stoppet først."
    )
    fun resetKafkaOffset(@RequestBody request: KafkaResetOffsetRequest): ResponseEntity<String> {
        sjekkTilgangTilKafkaAdmin()
        kafkaAdminService.resetOffsets(request, hentNavIdentEllerUkjent())
        return ResponseEntity.ok("Offsets oppdatert")
    }

    internal fun sjekkTilgangTilKafkaAdmin() {
        val applikasjon = hentApplikasjonFraContex(authContextHolder)
        if (!authContextHolder.erInternBruker()) {
            secureLog.warn(
                "Avvist tilgang til Kafka-admin (ikke intern bruker) for ident=${hentNavIdentEllerUkjent()}, azp_name=$applikasjon"
            )
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }

        val erPoaoAdmin = poaoAdmin == applikasjon
        val harTeamOboGruppe = harAdGruppe(TEAM_OBO_PROD_GROUP_ID)
        val erDevGcp = EnvironmentUtils.getOptionalProperty("NAIS_CLUSTER_NAME").orElse("") == "dev-gcp"
        val erTokenGeneratorIDev = erDevGcp && AZURE_TOKEN_GENERATOR_DEV == applikasjon

        if (!harTeamOboGruppe && !erPoaoAdmin && !erTokenGeneratorIDev) {
            secureLog.warn(
                "Avvist tilgang til Kafka-admin for ident=${hentNavIdentEllerUkjent()}, azp_name=$applikasjon"
            )
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
    }

    private fun harAdGruppe(gruppeId: String): Boolean =
        try {
            authContextHolder.idTokenClaims
                .orElse(null)
                ?.getStringListClaim("groups")
                ?.contains(gruppeId) == true
        } catch (e: Exception) {
            false
        }

    private fun hentNavIdentEllerUkjent(): String =
        authContextHolder.navIdent.orElse(null)?.get() ?: "ukjent"
}
