package no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.ZonedDateTime
import java.util.UUID

data class KontorDto(
    val kontorNavn: String,
    val kontorId: String,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "hendelseType")
@JsonSubTypes(
    JsonSubTypes.Type(value = GjeldendeOppfolgingsperiodeV2Dto::class, name = "OPPFOLGING_STARTET"),
    JsonSubTypes.Type(value = GjeldendeOppfolgingsperiodeV2Dto::class, name = "ARBEIDSOPPFOLGINGSKONTOR_ENDRET"),
    JsonSubTypes.Type(value = AvsluttetOppfolgingsperiodeV2::class, name = "OPPFOLGING_AVSLUTTET")
)
abstract class SisteOppfolgingsperiodeV2Dto(
    val oppfolgingsperiodeUuid: UUID,
    val sisteEndringsType: SisteEndringsType,
    val aktorId: String,
    val ident: String,
    val startTidspunkt: ZonedDateTime,
    val sluttTidspunkt: ZonedDateTime?,
    val kontor: KontorDto?,
    val producerTimestamp: ZonedDateTime = ZonedDateTime.now(),
)
