package no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.ZonedDateTime
import java.util.UUID

data class KontorDto(
    val kontorNavn: String,
    val kontorId: String,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "sisteEndringsType", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = GjeldendeOppfolgingsperiodeV3Dto::class, name = "OPPFOLGING_STARTET"),
    JsonSubTypes.Type(value = GjeldendeOppfolgingsperiodeV3Dto::class, name = "ARBEIDSOPPFOLGINGSKONTOR_ENDRET"),
    JsonSubTypes.Type(value = AvsluttetOppfolgingsperiodeV3Dto::class, name = "OPPFOLGING_AVSLUTTET")
)
abstract class SisteOppfolgingsperiodeV3Dto(
    val oppfolgingsperiodeUuid: UUID,
    val sisteEndringsType: SisteEndringsType,
    val aktorId: String,
    val ident: String,
    val startTidspunkt: ZonedDateTime,
    open val sluttTidspunkt: ZonedDateTime?,
    open val kontor: KontorDto?,
    val producerTimestamp: ZonedDateTime = ZonedDateTime.now(),
)
