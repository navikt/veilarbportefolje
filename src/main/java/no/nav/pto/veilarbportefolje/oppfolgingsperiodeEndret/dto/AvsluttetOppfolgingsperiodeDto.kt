package no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto

import java.time.ZonedDateTime
import java.util.UUID

class AvsluttetOppfolgingsperiodeV2(
    oppfolgingsperiodeUuid: UUID,
    startTidspunkt: ZonedDateTime,
    val sluttTidspunkt: ZonedDateTime,
    aktorId: String,
    ident: String,
    producerTimestamp: ZonedDateTime,
) : SisteOppfolgingsperiodeV2Dto(
    oppfolgingsperiodeUuid, startTidspunkt, SisteEndringsType.OPPFOLGING_AVSLUTTET, aktorId, ident, producerTimestamp,
)